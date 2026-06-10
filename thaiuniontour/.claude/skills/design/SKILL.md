---
name: design
description: thaiuniontour(태국 여행상품 판매 사이트) 설계를 7단계로 차근차근 진행하는 워크플로우. 요구사항부터 도메인 모델, ERD, API, 화면, 인프라(Docker), 백로그까지 단계별 산출물을 docs/design/ 에 작성한다. "설계", "design", "기획" 키워드에서 트리거.
---

# thaiuniontour 설계 스킬

태국 여행상품을 판매하는 커머스 웹사이트 **thaiuniontour**의 설계를
**한 단계씩(step-by-step)** 진행하기 위한 워크플로우다.
athometrip 프로젝트를 레퍼런스로 삼되, thaiuniontour에 맞게 단순화/재설계한다.

## 기술 스택 (확정)

| 영역 | 선택 | 비고 |
|------|------|------|
| 백엔드 | Spring Boot 3.x / Java 17 / Gradle 멀티모듈 | `api · domain · common` 3모듈로 시작 |
| 프론트 | Next.js 14 App Router / React 18 / TypeScript / Tailwind | athometrip과 동일 스택 |
| **DB** | **PostgreSQL 16** | UUID PK, TIMESTAMPTZ, JSONB, 스키마-per-도메인 |
| **메시징/락** | **Redis (Streams + 카운터/분산락)** | 이벤트 브로커 + 선착순 동시성 제어 |
| 배포 | Docker / docker-compose | `docker/` 폴더에 정의 |
| 구조 | 모노레포 | `backend/ · frontend/ · docker/ · docs/` |

> 레퍼런스: athometrip(커머스 도메인/계층 패턴) + **FlashArena(동시성·이벤트 결제 패턴)**.
> 동시성·이벤트 설계의 상세 패턴은 `docs/design/reference-patterns.md` 참조(필독).

## 백엔드 아키텍처 원칙

### 설계 스타일 — 선택적(전술적) DDD + 확장 seam (표준)
**핵심 도메인만 DDD, 나머지는 경량.** 단, 경량 컨텍스트도 나중에 DDD로 "승격"할 수 있게 구조를 열어둔다.

| Bounded Context | 스타일 |
|---|---|
| **Reservation, Payment** | 전술적 DDD: Aggregate · VO · Domain Event · Repository(포트) |
| Catalog/Product(전시), Label/i18n, CommonCode | 경량 Service+Repository (athometrip식) |
| Member | 중간 (권한/게스트만 VO) |

- 전술 요소: Aggregate(행위 보유: `Reservation.confirm()/cancel()`), VO(`Money(KRW)`·`Email`·`Phone`·`ReservationStatus`·`PaymentMethod`), **Domain Event = Outbox 이벤트**, Repository는 domain의 **포트 인터페이스** + infrastructure 어댑터.
- 재고 일관성은 **순수주의("트랜잭션당 애그리거트 1개")보다 oversell 0% 우선** — 재고 원자 UPDATE를 예약 트랜잭션에 포함(강한 일관성 가드).
- ❌ 이벤트소싱/전면 CQRS/전 컨텍스트 애그리거트 = 오버엔지니어링 금지.

**확장 seam (코드 작성 시 반드시 열어둘 지점):**
1. **결제수단**: `PaymentGateway` 포트 + `PaymentMethod` 공통코드. 신규 수단 = 코드 1건 + 어댑터 1개(기존 무수정). 입금확인도 `PaymentConfirmation` 전략(수동→자동 펌뱅킹 교체 가능).
2. **재고 차감 전략**: `StockDeductionStrategy` 포트. 기본 `AtomicUpdateStrategy`, 폭주 특가용 `RedisCounterStrategy`를 무수정 추가.
3. **도메인 이벤트**: Outbox `event_type` + payload에 **`version` 필드** 부여(스키마 진화 대비). 구독자 추가는 Consumer 추가만.
4. **경량 컨텍스트 승격**: 처음부터 **package-by-context**로 분리(전시/라벨도 자체 패키지). 도메인 로직을 컨트롤러에 두지 말 것 → 나중에 Aggregate 도입 시 호출부 무변경.
5. **좌석 임시점유(hold/TTL)**: 예약 생성에 hold 단계를 끼울 수 있도록 상태/포트 자리만 예약.

### 패키지 구조 (컨텍스트별 4계층)
```
domain/<context>/
  ├─ presentation/    REST(api) · Thymeleaf(admin)
  ├─ application/     유스케이스 서비스 = 트랜잭션 경계
  ├─ domain/          Aggregate · VO · DomainEvent · Repository(포트)
  └─ infrastructure/  JPA 어댑터 · Outbox · PG ACL
```

### 도메인 격리 + 이벤트 기반 (FlashArena 계승)
- **스키마-per-도메인**: 도메인마다 PostgreSQL 스키마 분리, **교차 스키마 물리 FK 금지**(UUID 타입 참조만).
- 도메인 간 통신은 **직접 호출이 아니라 이벤트**로 한다(주문 → 결제 등).
- 모듈 책임:
  - `common`: 공통 유틸·Enum·예외·응답 포맷·설정
  - `domain`: 엔티티·Repository·도메인 서비스 (스키마별 패키지: `reservation` / `payment` / `product` / `member` …)
  - `api`: 고객 대면 REST 컨트롤러 (Next.js 프론트가 소비)
  - `admin`: 운영자용 **Thymeleaf 서버렌더** 모듈 (상품 등록·상세 작성·재고 설정·입금 수동승인) — athometrip 참고
  - (확장 시) `scheduler`(Outbox 릴레이어) / `externalapi`(PG 연동) 추가

### 동시성(선착순) 원칙 — 한정 좌석/특가
- 재고/좌석 차감은 **read-modify-write 금지**. 다음 중 하나로:
  - **Redis 카운터(DECR) 게이트 + DB 원자 UPDATE** (고트래픽 선착순 기본값)
  - **원자 UPDATE** `... SET stock = stock - 1 WHERE stock > 0` (단순 케이스)
  - **낙관적 락**(`@Version`) — 충돌 드문 경우
- 상세 비교/선택 기준은 reference-patterns.md.

### 이벤트 기반 결제 원칙
- 예약(주문) 생성 시 **Transactional Outbox**: `reservation` INSERT 와 `outbox` INSERT를 **같은 로컬 트랜잭션**에서 원자 커밋(이벤트 유실 0%). Redis는 트랜잭션 밖에서 건드림(dual-write 회피).
- **Message Relayer**가 `outbox(READY)`를 `FOR UPDATE SKIP LOCKED`로 폴링→ Redis Stream `XADD` → `PUBLISHED` 전이, 실패는 `retry_count`++ 후 상한 초과 시 `FAILED`(준-DLQ).
- **결제 Consumer는 멱등**: Inbox 테이블 `UNIQUE(order_id)` + `exists` 빠른 경로 → 중복 이벤트 차단(exactly-once 효과), 정상/중복만 `XACK`.

---

## 핵심 원칙

1. **한 번에 한 단계만.** 단계가 끝나면 사용자에게 산출물을 보여주고 **확인을 받은 뒤** 다음 단계로 간다.
2. **모든 산출물은 `docs/design/` 에 마크다운으로 기록**한다. 머릿속에만 두지 않는다.
3. **결정에는 근거(왜)를 남긴다.** 대안과 트레이드오프를 짧게라도 적는다.
4. 추측이 필요하면 **사용자에게 질문**한다 (AskUserQuestion 활용). 임의로 가정하지 않는다.
5. 각 단계 시작 시 **이전 단계 산출물을 먼저 읽는다.**

---

## 설계 7단계

진행 상태는 `docs/design/00-OVERVIEW.md` 의 체크리스트로 추적한다.

### 1단계 — 요구사항 정의 → `docs/design/01-requirements.md`
- 비즈니스 목표, 타깃 사용자(여행객/관리자), 핵심 시나리오
- 기능 요구사항(상품 탐색·예약·결제·문의 등) / 비기능(성능·다국어·SEO·결제 보안)
- 범위(MVP vs 추후), 가정·제약
- **질문할 것**: 결제 수단(국내 PG/해외카드/현지), 다국어(한/영/태) 범위, 회원 정책

### 2단계 — 도메인 모델 & 용어사전 → `docs/design/02-domain.md`
- 핵심 도메인 객체: 상품(투어/패키지), 옵션/일정, 예약, 주문/결제, 회원, 리뷰, 지역/카테고리
- 용어사전(Ubiquitous Language) 한/영 매핑
- 도메인 간 관계, 상태(예약 상태머신 등)

### 3단계 — 데이터 모델(ERD) → `docs/design/03-erd.md`
- **스키마-per-도메인** 분리(`reservation` / `payment` / `product` / `member` …), 교차 스키마 FK 금지
- 테이블 정의(컬럼·타입·제약·인덱스), 관계(1:N, N:M)
- Mermaid `erDiagram` 으로 다이어그램 작성
- PostgreSQL 표준: PK `UUID DEFAULT gen_random_uuid()`, `TIMESTAMPTZ`, JSONB(이벤트 페이로드)
- 명명 규칙: snake_case 테이블/컬럼, PK `id`, FK `xxx_id`
- **동시성 자원 식별**: 재고/좌석 컬럼(`stock`/`seat_count`)과 차감 전략, 필요시 `version` 컬럼
- **이벤트 인프라 테이블**: 생산자 스키마의 `outbox`(부분 인덱스 `WHERE status='READY'`), 소비자 스키마의 inbox(`UNIQUE(order_id)`)

### 4단계 — API 설계 → `docs/design/04-api.md`
- 리소스별 엔드포인트 표(메서드·경로·요청·응답·권한)
- 공통 응답 포맷, 에러 코드 체계, 페이지네이션 규칙
- 인증/인가 방식(JWT 등) 결정
- DTO 네이밍은 athometrip 규칙 계승
- **예약→결제 플로우 정의**: 동기 응답 경계(예약 생성까지)와 비동기 이벤트 경계(결제 처리) 구분, 결제 상태 폴링/조회 엔드포인트
- **PG(외부 결제) 연동 지점**: 콜백/웹훅 멱등 처리 방침

### 5단계 — 화면/IA 설계 → `docs/design/05-frontend.md`
- 고객 프론트는 **athometrip-front UI를 참고**하되 **사용성 개선**(athometrip의 하드코딩 라우트·검색 부재를 탈피):
  - **GNB 메가메뉴 = 데이터 기반 테마 3단계 카테고리** (admin이 관리하는 `category` 트리에서 렌더)
  - **항상 노출되는 글로벌 검색바** (자동완성, 풀텍스트)
  - **목록 페이지 필터 사이드바**: 지역(패싯)·가격·테마 + 정렬, **브레드크럼**(3단계)
  - 카테고리/검색 라우트는 동적(`/c/[...slug]`, `/search`)으로 — 하드코딩 금지
- 사이트맵·페이지 목록, App Router 라우팅 구조(`app/` 트리)
- 페이지별 컴포넌트 구성, 상태관리(Zustand 등), API 연동 지점
- 반응형/다국어(ko/en) 전략, SEO(메타데이터) 방침

### 6단계 — 아키텍처 & 인프라 → `docs/design/06-infra.md`
- 시스템 구성도(Mermaid): 예약→Outbox→Relayer→Redis Stream→결제 Consumer 흐름 포함
- 모듈 의존 관계, 도메인 격리 경계
- Docker 구성: 서비스(backend·frontend·**postgres·redis**·nginx), `docker-compose.yml` 설계
- 환경 분리(local/dev/prod), 환경변수, 빌드/배포 파이프라인 개요
- Outbox 릴레이어 배치 주기, Redis Stream consumer group, 장애 복구(재시도/DLQ) 운영 방침

### 7단계 — 백로그 & 마일스톤 → `docs/design/07-backlog.md`
- 기능을 작업 단위로 분해, 우선순위(MVP → v1 → v2)
- 마일스톤별 묶음, 의존관계, 대략적 순서
- 다음 액션(구현 착수 지점) 명시

---

## 진행 방법

1. `docs/design/00-OVERVIEW.md` 를 읽어 현재 단계 확인
2. 해당 단계의 이전 산출물 읽기
3. 부족한 정보는 사용자에게 질문
4. 산출물 작성 → 사용자에게 요약 보고
5. 확인받으면 `00-OVERVIEW.md` 체크리스트 갱신 후 다음 단계로

설계가 모두 끝나면 구현 단계로 넘어간다(이 스킬의 범위 밖).
