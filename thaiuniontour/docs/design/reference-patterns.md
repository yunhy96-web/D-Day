# 레퍼런스 패턴 — 동시성(선착순) & 이벤트 기반 결제

thaiuniontour의 핵심 비기능 요구(한정 좌석 선착순·동시성 안전·이벤트 기반 결제)는
**FlashArena** 프로젝트(`../FlashArena`, 같은 D-Day 저장소 내 형제 폴더)에서 검증된 패턴을 이식한다.
이 문서는 설계 단계에서 참조하는 패턴 카탈로그다. (구현 시 실제 코드는 이 원칙을 따른다.)

---

## 1. 도메인 격리 (스키마-per-도메인)

- PostgreSQL **스키마**로 도메인 경계를 물리적으로 분리: `reservation`, `payment`, `catalog`(상품/분류/재고), `member`, `common` …
- **교차 스키마 물리 FK 금지.** 다른 도메인의 식별자는 **UUID 타입으로만** 참조한다.
- 도메인 간 통신은 직접 호출이 아니라 **이벤트**로 한다.
  - 예: 예약 도메인이 결제 도메인을 직접 호출하지 않는다. 예약 완료 이벤트만 발행하고, 결제는 그걸 구독한다.
  - 결제 도메인은 예약 스키마/outbox의 존재조차 모른다 (Redis 큐만 구독).

---

## 2. 동시성(선착순) — 재고/좌석 차감 전략

한정 좌석 투어·특가 상품의 `stock`/`seat_count` 가 Race Condition의 표적.
**read-modify-write 절대 금지** (lost-update → oversell/음수 재고).

| 전략 | 방법 | 적합한 상황 |
|------|------|-------------|
| ❌ No-Lock | 읽고→차감→저장 | (안티패턴) oversell 시연용일 뿐, 실사용 금지 |
| 낙관적 락 | `@Version` 컬럼, 충돌 시 재시도 | 동시 충돌이 **드문** 일반 예약 |
| 원자 UPDATE | `UPDATE ... SET stock = stock - 1 WHERE id=? AND stock > 0` | 단순·중간 트래픽. 1건이라도 못 줄이면 매진 |
| **Redis 카운터 게이트** | Redis `DECR` 로 먼저 판정(당첨/낙첨) → 당첨자만 DB 원자 UPDATE | **고트래픽 선착순**(특가 오픈런) 기본값 |
| 분산 락 | Redis 분산락으로 임계영역 직렬화 | 복합 차감 로직을 단순하게 직렬화해야 할 때 |

### 권장 기본값
- **일반 투어 예약**: 원자 UPDATE (`WHERE stock > 0`) — 단순하고 정확.
- **특가/오픈런(선착순 폭주)**: Redis 카운터 게이트 + DB 원자 UPDATE.
  - 게이트(DECR)가 음수 차감을 막으므로 DB UPDATE에 `WHERE stock>0` 가드 불필요.
  - DB는 "확정된 당첨자만" 받으므로 DB 부하·락 경합이 급감.

```sql
-- 원자 차감 (JPA @Modifying @Query 또는 네이티브)
UPDATE product.product SET stock = stock - 1
 WHERE id = :id AND stock > 0;   -- 영향 행 0 → 매진(FAILED 처리)
```

---

## 3. 이벤트 기반 결제 — Transactional Outbox → Relay → 멱등 Consumer

### 전체 흐름
```
[예약 생성 트랜잭션]                         [메시지 릴레이]            [결제 도메인]
reservation INSERT  ┐                       READY outbox 폴링         XREAD 구독
outbox     INSERT   ┘ 같은 로컬 TX 원자 커밋  (FOR UPDATE SKIP LOCKED)   → 멱등 INSERT
                                            → Redis XADD              → UNIQUE(order_id)
                                            → PUBLISHED 전이           → XACK
```

### 3-1. 생산자: Transactional Outbox
- `reservation` 스키마가 outbox를 **소유**(생산자 소유 원칙).
- 예약 성공 시 **세 작업을 같은 트랜잭션에서 원자 커밋**:
  1. 재고/좌석 차감
  2. `reservation` INSERT (status=CONFIRMED)
  3. `outbox` INSERT (event_type=`RESERVATION_CONFIRMED`, payload=JSONB)
- ⇒ "예약은 있는데 이벤트가 없는" 상태가 **구조적으로 불가능**.
- ⚠️ 이 트랜잭션 안에서 **Redis를 절대 호출하지 않는다** (dual-write 회피).

```sql
CREATE TABLE reservation.outbox (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'RESERVATION',
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(50) NOT NULL,
    payload        JSONB NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'READY',  -- READY/PUBLISHED/FAILED
    retry_count    INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
-- 릴레이어 폴링용 부분 인덱스
CREATE INDEX idx_outbox_ready ON reservation.outbox (created_at) WHERE status = 'READY';
```

### 3-2. 메시지 릴레이어 (`scheduler` 또는 api)
- `@Scheduled` 주기 폴링. 각 틱은 자체 트랜잭션:
  1. `findReadyBatchForUpdate` — **`FOR UPDATE SKIP LOCKED`** 로 잠금 조회(중복 발행/멀티 인스턴스 안전).
  2. 건별 Redis `XADD` (Stream).
  3. `XADD` 성공(RecordId≠null) → `PUBLISHED` + `published_at`.
  4. 실패 → `retry_count`++; 상한 초과 → `FAILED`(준-DLQ), 나머지는 READY 유지 후 다음 틱 재시도.
- ⚠️ XADD 성공 = "스트림이 적재함"일 뿐 "소비자가 처리함"이 아니다. 소비자 멱등성은 별도(3-3).

### 3-3. 소비자: 멱등 결제 처리 (Inbox)
- `payment` 스키마가 inbox(처리 이력) 소유. **`UNIQUE(order_id)`** 가 핵심 방어선.
- 처리 순서:
  1. `existsByOrderId` 빠른 경로 → 이미 처리됨이면 스킵(+XACK).
  2. INSERT 시도 → `UNIQUE` 위반(`DataIntegrityViolationException`)이면 동시 중복으로 간주, 스킵(+XACK).
  3. 정상 처리 → XACK.
  4. 비-중복 오류 → 예외 throw, **XACK 하지 않음** → PEL에 남아 재전달.
- ⇒ at-least-once 발행 + UNIQUE inbox = **exactly-once 효과**.

```sql
CREATE TABLE payment.payment_history (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id  UUID NOT NULL,            -- 멱등성 키
    amount    BIGINT NOT NULL DEFAULT 0,
    status    VARCHAR(20) NOT NULL DEFAULT 'PAID',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payment_history_order_id ON payment.payment_history (order_id);
```

---

## 4. thaiuniontour 적용 시 추가 고려 (FlashArena와의 차이)

- FlashArena 결제는 **시뮬레이션**. thaiuniontour는 **실제 PG 연동**이 필요.
  - PG 승인은 외부 호출이므로 `externalapi` 도메인으로 격리, **웹훅/콜백도 멱등** 처리(동일 패턴).
  - "예약 확정(좌석 점유)" → "결제 대기/승인" → "결제 완료/취소" 상태 흐름을 이벤트로 모델링.
  - 결제 실패/타임아웃 시 **좌석 보상 트랜잭션(재고 복원)** 이벤트 설계 필요(SAGA).
- 좌석 **임시 점유(holding) + TTL** 패턴 검토: 결제 진행 중 좌석 선점, 만료 시 자동 반환(Redis TTL).
- 멀티 인스턴스 배포 시 Redis Stream **consumer group**으로 결제 컨슈머 수평 확장.

---

## 5. 설계 시 반드시 결정할 것 (이벤트/동시성 관련 미결 항목)
- [ ] 선착순 대상 상품 유형(특가만? 모든 한정 좌석?)과 기본 차감 전략
- [ ] 좌석 임시 점유(holding) 도입 여부 + TTL
- [ ] 이벤트 종류 카탈로그: `RESERVATION_CONFIRMED`, `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `RESERVATION_CANCELLED` …
- [ ] 결제 실패 시 보상(좌석 복원) SAGA 흐름
- [ ] Redis Stream 키/consumer group 네이밍, 릴레이 주기, 재시도 상한
- [ ] PG 사 선정 및 웹훅 멱등 키
