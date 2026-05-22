-- =============================================================
-- FlashArena :: V3 - "order" 스키마 (동시성 시뮬레이터 핵심)
-- -------------------------------------------------------------
-- product.quantity 가 Race Condition 의 대상이 되는 핵심 자원.
-- ⚠️ "order" 는 예약어 → 항상 큰따옴표.
-- =============================================================

-- 상품: quantity(재고) 가 동시성 차감의 표적
CREATE TABLE IF NOT EXISTS "order".product (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL DEFAULT 0,
    quantity    INT          NOT NULL,            -- 재고. SYNC 모드에서 음수까지 깨질 수 있음
    version     BIGINT       NOT NULL DEFAULT 0,  -- (예약) 낙관적 락 모드 확장용
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON COLUMN "order".product.quantity IS '재고. No-Lock 모드에서 음수로 깨지는 것을 시연';
COMMENT ON COLUMN "order".product.version  IS '낙관적 락 확장 대비 컬럼 (Phase 3 기본 모드에선 미사용)';

-- 주문 내역
CREATE TABLE IF NOT EXISTS "order".orders (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,            -- auth.users.id 를 "타입으로만" 참조 (FK 없음)
    product_id  UUID         NOT NULL,            -- 같은 스키마 → FK 허용
    quantity    INT          NOT NULL DEFAULT 1,
    status      VARCHAR(20)  NOT NULL DEFAULT 'CREATED',  -- CREATED / FAILED
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 동일 스키마 내 FK 는 허용 (스키마 '간' FK 만 금지)
    CONSTRAINT fk_orders_product
        FOREIGN KEY (product_id) REFERENCES "order".product (id)
);

CREATE INDEX IF NOT EXISTS idx_orders_product_id ON "order".orders (product_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id    ON "order".orders (user_id);

COMMENT ON COLUMN "order".orders.user_id IS 'auth.users.id 참조. 물리 FK 없이 UUID 타입으로만 연결 (도메인 격리)';
COMMENT ON COLUMN "order".orders.status  IS 'CREATED(성공) / FAILED(재고부족 등)';

-- -------------------------------------------------------------
-- Transactional Outbox (이벤트 "생산자"인 order 도메인이 소유)
-- 핵심: orders INSERT 와 outbox INSERT 가 같은 order-스키마 로컬 트랜잭션에서
--       원자적으로 커밋된다 → 교차 스키마 트랜잭션 없이 이벤트 유실 0%.
-- payment 도메인은 이 테이블의 존재조차 모른다 (Redis 큐만 구독).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "order".outbox (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50)  NOT NULL DEFAULT 'ORDER',  -- 이벤트 발생 애그리거트
    aggregate_id    UUID         NOT NULL,                  -- order id (타입 참조)
    event_type      VARCHAR(50)  NOT NULL,                  -- 예: ORDER_COMPLETED
    payload         JSONB        NOT NULL,                  -- 이벤트 페이로드
    status          VARCHAR(20)  NOT NULL DEFAULT 'READY',  -- READY / PUBLISHED / FAILED
    retry_count     INT          NOT NULL DEFAULT 0,        -- 재시도 상한(준-DLQ)용
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);

-- 릴레이어가 READY 건만 빠르게 폴링하기 위한 부분 인덱스
CREATE INDEX IF NOT EXISTS idx_outbox_status_ready
    ON "order".outbox (created_at)
    WHERE status = 'READY';

COMMENT ON TABLE  "order".outbox         IS '주문 트랜잭션과 원자적으로 기록되는 이벤트 아웃박스 (생산자=order 소유)';
COMMENT ON COLUMN "order".outbox.payload IS 'JSONB 이벤트 페이로드 (orderId, userId, amount 등)';
COMMENT ON COLUMN "order".outbox.status  IS 'READY→릴레이어 발행 대상 / PUBLISHED→Redis(XADD) 적재 완료 / FAILED→재시도 초과';
