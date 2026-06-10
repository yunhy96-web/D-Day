# CLAUDE.md — thaiuniontour

태국 여행상품(투어·패키지)을 판매하는 커머스 웹사이트. athometrip을 레퍼런스로 새로 설계·구현한다.

## 현재 단계
**설계(Design)** 진행 중. 구현 전, `docs/design/` 의 7단계 설계를 먼저 완료한다.
설계는 `.claude/skills/design` 스킬로 단계별 진행하며, 진행 현황은 `docs/design/00-OVERVIEW.md` 참조.

## 기술 스택
- **백엔드**: Spring Boot 3.x / Java 17 / Gradle 멀티모듈 — `api · domain · common` (확장 시 admin/scheduler/externalapi)
- **프론트**: Next.js 14 App Router / React 18 / TypeScript / Tailwind
- **DB**: PostgreSQL 16 (UUID PK · TIMESTAMPTZ · JSONB · 스키마-per-도메인)
- **메시징/락**: Redis (Streams 이벤트 브로커 + 선착순 동시성 제어)
- **배포**: Docker · docker-compose / **구조**: 모노레포 — `backend/ · frontend/ · docker/ · docs/`

## 백엔드 규칙
- 계층(athometrip 계승): **Controller → Service → Repository → DB**, Request DTO 용도별 분리(`ReqXxxRegist`/`ReqXxxModify`/`ReqXxx`)
- 모듈 책임: `common`(유틸·Enum·예외·응답·설정) / `domain`(엔티티·Repository·도메인서비스, 스키마별 패키지) / `api`(고객 REST)
- **도메인 격리**(FlashArena 계승): 스키마-per-도메인, 교차 스키마 FK 금지(UUID 타입 참조), 도메인 간 통신은 **이벤트**로만
- **동시성/선착순**: 재고·좌석 차감은 read-modify-write 금지 → 원자 UPDATE / Redis 카운터 게이트 / 낙관적 락
- **이벤트 결제**: Transactional Outbox(예약+outbox 원자 커밋) → Relayer(`FOR UPDATE SKIP LOCKED` + Redis XADD) → 멱등 Consumer(Inbox `UNIQUE(order_id)`)
- 동시성·이벤트 상세 패턴: `docs/design/reference-patterns.md` (필독)

## 백엔드 컨벤션 (M0 확정)
- **엔티티 스키마 명시**: 전역 `hibernate.default_schema` 미사용. 모든 JPA 엔티티는 `@Table(schema="catalog"|"reservation"|"payment"|"member")` 명시 필수(스키마-per-도메인).
- **Flyway 소유권**: 마이그레이션은 **api 모듈만** 소유(`db/migration`). admin·(추후 scheduler/externalapi 등) **모든 다른 모듈은 `spring.flyway.enabled: false`** — 중복 실행 방지.
- **domain 패키지 = 컨텍스트별 4계층**: `domain.<context>.{model,repository,service,port}`. 컨텍스트: `catalog`/`reservation`/`payment`/`member`. 도메인 로직을 컨트롤러에 두지 말 것(경량 컨텍스트 승격 seam).
- **확장 seam 포트 위치**: `StockDeductionStrategy`(catalog.port), `PaymentGateway`·`PaymentConfirmation`(payment.port), `FileStorage`(common.storage). 신규 결제수단/재고전략 = 어댑터 1개 추가(기존 무수정).
- **DDD 베이스**(common.domain): `AggregateRoot`(도메인이벤트 수집), `DomainEvent`(`schemaVersion`=Outbox payload version), `Money`(KRW VO).

## 작업 원칙
- 설계가 끝나기 전에는 구현 코드를 작성하지 않는다.
- 모든 설계 결정은 근거와 함께 `docs/design/` 에 기록한다.
