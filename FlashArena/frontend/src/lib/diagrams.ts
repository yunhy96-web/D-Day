/**
 * 포트폴리오 설명용 Mermaid 다이어그램 정의 모음.
 * <Mermaid chart={...} /> 에 그대로 전달해 렌더링한다.
 */

/** A. 전체 아키텍처. */
export const ARCHITECTURE = `flowchart LR
    Browser["🌐 브라우저<br/>(EventSource)"]

    subgraph App["FlashArena Backend (Spring Boot)"]
        direction TB
        Sim["SimulationService<br/>동시성 시뮬레이터"]
        OP["OrderProcessor<br/>재고차감+주문+outbox<br/>원자 커밋"]
        Relay["OutboxRelayer<br/>@Scheduled 0.5s"]
        Consumer["PaymentConsumer<br/>멱등 소비 → XACK"]
    end

    subgraph PG["PostgreSQL"]
        direction TB
        Order["schema order<br/>product · orders · outbox"]
        Pay["schema payment<br/>payment_history"]
    end

    subgraph R["Redis"]
        Stream["Stream<br/>flasharena:order-events"]
        Lock["Redisson 분산락"]
    end

    Browser -->|"POST /run → 202 + runId"| Sim
    Browser <-->|"GET /stream/:runId<br/>event: log / result"| Sim
    Sim --> OP
    Sim -.->|"REDIS_LOCK 모드"| Lock
    OP -->|"한 트랜잭션"| Order
    Relay -->|"① READY SELECT<br/>FOR UPDATE SKIP LOCKED"| Order
    Relay -->|"② XADD"| Stream
    Stream -->|"③ XREADGROUP"| Consumer
    Consumer -->|"UNIQUE order_id 멱등 INSERT"| Pay`

/** E. Redis 카운터(DECR) 게이트키핑 — 락 없이 당첨자만 DB로. */
export const COUNTER_GATEKEEPING = `flowchart TB
    R["요청 1000건<br/>(초기 재고 100)"] --> D{"Redis 원자 DECR<br/>stock:product:id"}
    D -->|"잔여 >= 0 · 100명"| W["✅ 당첨<br/>DB 원자 차감 + 주문·아웃박스 기록"]
    D -->|"잔여 < 0 · 900명"| L["❌ 낙첨<br/>즉시 거절 (DB·락 안 거침)"]
    W --> DB[("PostgreSQL<br/>당첨자 100건만 도달")]

    subgraph cmp["락 모드와의 차이"]
        direction LR
        lock["REDIS_LOCK<br/>1000명 전원이 단일 락에 줄섬<br/>(낙첨자도 락 잡아 확인) → 느림"]
        counter["REDIS_COUNTER<br/>낙첨자는 DECR 한 번에 거절<br/>DB는 당첨자만 → 빠름"]
    end`

/** D. 모듈러 모놀리스 → MSA 분리 경로. */
export const MODULAR_MONOLITH = `flowchart TB
    subgraph now["현재 — 모듈러 모놀리스 (단일 배포)"]
        direction LR
        O["order 모듈<br/>schema order"]
        P["payment 모듈<br/>schema payment"]
        A["auth 모듈<br/>schema auth"]
        O -. "이벤트만<br/>(Redis Stream)" .-> P
    end

    Bus(["Redis Stream<br/>(도메인 간 유일한 통신 채널)"])
    now --- Bus

    subgraph later["분리 시 — 독립 서비스 (경계 그대로 승격)"]
        direction LR
        OS["Order Service<br/>own DB"]
        PS["Payment Service<br/>own DB"]
        AS["Auth Service<br/>own DB"]
        OS -. "이벤트" .-> PS
    end

    now ==>|"스키마 분리 · FK 없음 · 이벤트 통신을<br/>이미 지켰으므로 추출이 안전"| later`

/** B-1. SYNC 모드 — lost update / oversell 재현. */
export const CONCURRENCY_SYNC = `sequenceDiagram
    autonumber
    participant A as Worker A
    participant B as Worker B
    participant DB as product (재고=1)

    A->>DB: read quantity
    DB-->>A: 1
    B->>DB: read quantity
    DB-->>B: 1
    Note over A,B: 둘 다 같은 값(1)을 읽음 — 경쟁 창
    A->>DB: save quantity=0 (CREATED)
    B->>DB: save quantity=0 (CREATED)
    Note over DB: ❌ 성공 2건 > 재고 1 → 음수 재고 oversell`

/** B-2. REDIS_LOCK 모드 — 분산락으로 직렬화. */
export const CONCURRENCY_LOCK = `sequenceDiagram
    autonumber
    participant A as Worker A
    participant B as Worker B
    participant L as Redisson Lock
    participant DB as product (재고=1)

    A->>L: tryLock()
    B->>L: tryLock()
    L-->>A: 🔒 획득
    L-->>B: ⏳ 대기
    A->>DB: read=1 → save quantity=0 (CREATED)
    A->>L: 🔓 unlock
    L-->>B: 🔒 획득
    B->>DB: read quantity
    DB-->>B: 0
    B->>DB: 재고 부족 → FAILED
    Note over DB: ✅ 성공 1 = 재고 1 → oversell 0`

/** C. 이벤트 파이프라인 — Transactional Outbox & 멱등 소비. */
export const EVENT_PIPELINE = `sequenceDiagram
    autonumber
    participant OP as OrderProcessor
    participant DB as PostgreSQL (order)
    participant Relay as OutboxRelayer
    participant Stream as Redis Stream
    participant Con as PaymentConsumer
    participant Pay as PostgreSQL (payment)

    rect rgb(30, 41, 59)
    Note over OP,DB: ① 주문 트랜잭션 — 원자적 (한 커밋)
    OP->>DB: 재고 차감 + orders INSERT + outbox INSERT(READY)
    Note over OP,DB: Redis 안 건드림 → dual-write 회피
    end

    rect rgb(15, 42, 30)
    Note over Relay,Stream: ② 릴레이 — 0.5초 폴링 (별도 트랜잭션)
    Relay->>DB: READY SELECT … FOR UPDATE SKIP LOCKED
    DB-->>Relay: READY 이벤트 배치
    Relay->>Stream: XADD
    alt XADD 성공
        Relay->>DB: outbox → PUBLISHED
    else 실패
        Relay->>DB: retry_count++ (상한 초과 시 FAILED)
    end
    end

    rect rgb(42, 35, 15)
    Note over Con,Pay: ③ 멱등 소비 — at-least-once 방어
    Stream->>Con: XREADGROUP
    Con->>Pay: existsByOrderId?
    alt 이미 처리됨
        Con->>Stream: XACK (skip)
    else 신규
        Con->>Pay: INSERT (UNIQUE order_id = 최종 방어선)
        Con->>Stream: XACK
    end
    end`
