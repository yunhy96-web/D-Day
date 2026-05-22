# FlashArena — Phase 1: 초기 설정 가이드 & DB 스키마

> 대규모 트래픽 동시성 문제와 분산 환경 장애 대응 패턴을 **시각적으로 증명**하는
> 이커머스 시뮬레이터 대시보드. 면접관에게 "락 없으면 깨지고, 분산 락 쓰면 정확히 막힌다"를 실시간으로 보여준다.

## 1. 기술 스택 & 제약

| 영역 | 선택 | 비고 |
|---|---|---|
| Backend | **Java 21 / Spring Boot 3.x** | 스레드 풀(최대 50), Redisson 분산 락, `@Scheduled` 릴레이어 |
| Frontend | **Vite + React + TypeScript + Tailwind** | 단일 Playground 대시보드, SSE 실시간 로그 |
| Cache/Queue | **Redis 1대** | 분산 락 + 비동기 메시지 브로커 겸용 (메모리 절약) |
| Database | **PostgreSQL 1 인스턴스** | EC2 프리티어 RAM 1GB 제약 가정 |

### 핵심 제약 (절대 위반 금지)
- 단일 물리 DB 안에 `auth` / `order` / `payment` **3개 스키마**로 도메인 격리 (MSA 흉내).
- 모든 PK는 **UUID** (`gen_random_uuid()`, PG13+ 코어 내장).
- 스키마 **간** 물리 FK 금지 → **UUID 데이터타입으로만** 참조.
- 스키마 **간** DB JOIN / 단일 트랜잭션 공통 처리 **금지**.
  - 동일 스키마 **내부** FK(`order.orders → order.product`)는 허용.

> ⚠️ **`order` 는 SQL 예약어**다. SQL/JPA 어디서든 `"order"` 로 큰따옴표 처리.
> Spring 에선 `spring.jpa.properties.hibernate.globally_quoted_identifiers=true`
> 또는 `@Table(schema = "\"order\"")` 로 처리한다.

## 2. 디렉터리 구조 (목표)

```
FlashArena/
├── docs/                    # 가이드 문서
├── db/
│   ├── ddl/                 # V1~V4 스키마/테이블 DDL
│   └── seed/                # V5 시드 데이터
├── backend/                 # (Phase 2~) Spring Boot
└── frontend/                # (Phase 5)  Vite + React
```

## 3. 인프라 기동

공용 Docker PostgreSQL(`checkstock-postgres`) 위에 `flasharena` DB만 새로 만든다.
포트 충돌 회피: checkstock=8091 → **FlashArena backend = 8092**.

### 3-1. DB 생성
```bash
docker exec checkstock-postgres \
  psql -U sideproject -d postgres -c "CREATE DATABASE flasharena;"
```

### 3-2. DDL/시드 적용 (순서대로)
```bash
cd D-Day/FlashArena
for f in db/ddl/V1__schema.sql db/ddl/V2__auth.sql db/ddl/V3__order.sql \
         db/ddl/V4__payment.sql db/seed/V5__seed.sql; do
  echo ">> applying $f"
  docker exec -i checkstock-postgres psql -U sideproject -d flasharena < "$f"
done
```

> 운영 단계에선 Spring Boot **Flyway** 로 `V1~V5` 마이그레이션 자동화 권장
> (`ddl-auto: none` 로 두고 DDL을 단일 진실 소스로 유지).

### 3-3. Redis (분산 락 + 큐 겸용)
```bash
docker run -d --name flasharena-redis -p 6379:6379 redis:7-alpine
```

## 4. 생성되는 스키마 한눈에 보기

```
auth
└── users(id, username, password, role, created_at)        # 공용 계정

"order"
├── product(id, name, price, quantity, version, ...)        # 재고 = 동시성 표적
└── orders(id, user_id*, product_id→product, quantity, status, ...)

payment
├── outbox(id, aggregate_id*, event_type, payload jsonb, status, ...)
└── payment_history(id, order_id* UNIQUE, amount, status, ...)  # 멱등성
```
`*` = 다른 스키마를 UUID 타입으로만 참조 (물리 FK 없음)

## 5. 검증 쿼리
```bash
docker exec -i checkstock-postgres psql -U sideproject -d flasharena -c "\
  SELECT table_schema, table_name FROM information_schema.tables \
  WHERE table_schema IN ('auth','order','payment') ORDER BY 1,2;"
```
기대: `auth.users`, `order.orders`, `order.product`, `payment.outbox`, `payment.payment_history`

## 6. 다음 단계
- **Phase 2** — `auth`: 고정 계정 JWT 로그인 + 인터셉터로 UUID 전파
- **Phase 3** — `order`: SYNC vs REDIS_LOCK 동시성 시뮬레이터 (스레드 풀 50)
- **Phase 4** — `payment`: Transactional Outbox + 릴레이어 + 소비자 멱등성
- **Phase 5** — Frontend: Playground 대시보드 (SSE 실시간 로그 + 결과 리포트)
