-- =============================================================
-- V1__init.sql  — Hauly 초기 스키마
-- 의존 순서: app_user → customer → common_code_group/common_code
--           → i18n_message → product_category → product
--           → "order" → order_item → order_status_log
-- =============================================================

-- -------------------------
-- 1. 운영자 계정 (app_user)
-- -------------------------
CREATE TABLE app_user (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role          VARCHAR(16)  NOT NULL,    -- 'INTAKE' | 'BUYER' | 'ADMIN'
    display_name  VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -------------------------
-- 2. 고객 (customer)
-- -------------------------
CREATE TABLE customer (
    id               BIGSERIAL    PRIMARY KEY,
    account_status   VARCHAR(16)  NOT NULL,    -- 'GUEST' | 'REGISTERED'
    line_id          VARCHAR(64)  UNIQUE,       -- 게스트 매칭 키 1
    phone            VARCHAR(32)  UNIQUE,       -- 게스트 매칭 키 2 (E.164)
    email            VARCHAR(128) UNIQUE,       -- 회원 가입 시
    password_hash    VARCHAR(128),              -- 회원만, BCrypt
    name             VARCHAR(64)  NOT NULL,
    default_address  TEXT,
    preferred_lang   VARCHAR(8),               -- 'th' | 'en' | 'ko'
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -------------------------
-- 3. 공통코드 그룹 / 코드
-- -------------------------
CREATE TABLE common_code_group (
    group_code    VARCHAR(32) PRIMARY KEY,
    group_name_ko VARCHAR(64) NOT NULL,
    group_name_en VARCHAR(64),
    description   TEXT,
    is_system     BOOLEAN     NOT NULL DEFAULT false,  -- true 면 그룹 자체 삭제 불가
    is_active     BOOLEAN     NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE common_code (
    group_code VARCHAR(32) NOT NULL REFERENCES common_code_group(group_code),
    code       VARCHAR(32) NOT NULL,
    name_ko    VARCHAR(64) NOT NULL,
    name_en    VARCHAR(64),
    name_th    VARCHAR(64),
    sort_order INT         NOT NULL DEFAULT 0,
    is_system  BOOLEAN     NOT NULL DEFAULT false,  -- true 면 코드 삭제·이름변경만 가능
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    attributes JSONB,    -- UI 색상, 다음 가능 상태 목록 등 메타
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_code, code)
);

CREATE INDEX idx_common_code_group_sort
    ON common_code (group_code, sort_order, is_active);

-- -------------------------
-- 4. UI 다국어 메시지
-- -------------------------
CREATE TABLE i18n_message (
    message_key VARCHAR(128) NOT NULL,   -- 'btn.cancel', 'menu.orders', 'category.lens.brand'
    lang_code   VARCHAR(8)   NOT NULL,   -- common_code: LANG (ko/en/th)
    message     TEXT         NOT NULL,
    context     VARCHAR(64),             -- 'admin' | 'shop' | 'common' | 'category.lens'
    is_system   BOOLEAN      NOT NULL DEFAULT false,  -- 코어 UI 키는 삭제 불가
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (message_key, lang_code)
);

CREATE INDEX idx_i18n_context_key
    ON i18n_message (context, message_key);

-- -------------------------
-- 5. 상품 카테고리 (동적 필드 스키마)
-- -------------------------
CREATE TABLE product_category (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(64)  NOT NULL UNIQUE,    -- 'CONTACT_LENS', 'COSMETICS', 'GENERAL'
    parent_id    BIGINT       REFERENCES product_category(id),  -- 향후 트리 구조 대비
    name_key     VARCHAR(128) NOT NULL,            -- i18n_message key
    field_schema JSONB        NOT NULL DEFAULT '{"fields":[]}'::jsonb,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -------------------------
-- 6. 상품 (Phase 2 카탈로그)
-- -------------------------
CREATE TABLE product (
    id                   BIGSERIAL    PRIMARY KEY,
    sku                  VARCHAR(64)  NOT NULL UNIQUE,
    name_th              VARCHAR(255),
    name_en              VARCHAR(255),
    name_ko              VARCHAR(255),
    description_th       TEXT,
    description_en       TEXT,
    price_thb            NUMERIC(12, 2) NOT NULL,
    estimated_cost_krw   NUMERIC(12, 0),
    thumbnail_image_key  VARCHAR(255),
    image_keys           JSONB,
    status               VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',  -- 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -------------------------
-- 7. 주문 ("order" — 예약어이므로 쌍따옴표)
-- -------------------------
CREATE TABLE "order" (
    id                        BIGSERIAL     PRIMARY KEY,
    order_no                  VARCHAR(20)   NOT NULL UNIQUE,   -- 예: HL-2026-0001
    customer_id               BIGINT        REFERENCES customer(id),
    origin                    VARCHAR(32)   NOT NULL,          -- common_code: ORDER_ORIGIN
    fulfillment_status        VARCHAR(32)   NOT NULL,          -- common_code: FULFILLMENT_STATUS
    payment_status            VARCHAR(32)   NOT NULL,          -- common_code: PAYMENT_STATUS
    payment_method            VARCHAR(32),                     -- common_code: PAYMENT_METHOD
    customer_memo             TEXT,
    internal_memo             TEXT,
    total_request_thb         NUMERIC(12, 2),
    total_purchased_krw       NUMERIC(12, 0),
    exchange_rate             NUMERIC(8, 4),
    payment_receipt_image_key VARCHAR(255),
    payment_submitted_at      TIMESTAMPTZ,
    payment_confirmed_at      TIMESTAMPTZ,
    payment_confirmed_by      BIGINT        REFERENCES app_user(id),
    acknowledged_at           TIMESTAMPTZ,
    korean_tracking_no        VARCHAR(64),
    korean_courier            VARCHAR(32),                     -- common_code: COURIER_KR
    shipped_to_agent_at       TIMESTAMPTZ,
    completed_at              TIMESTAMPTZ,
    cancel_reason_code        VARCHAR(32),                     -- common_code: CANCEL_REASON
    reject_reason_code        VARCHAR(32),                     -- common_code: REJECT_REASON
    created_by                BIGINT        REFERENCES app_user(id),   -- INTAKE 사용자
    purchased_by              BIGINT        REFERENCES app_user(id),   -- BUYER 사용자
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_fulfillment_created
    ON "order" (fulfillment_status, created_at DESC);
CREATE INDEX idx_order_payment_created
    ON "order" (payment_status, created_at DESC);
CREATE INDEX idx_order_customer
    ON "order" (customer_id);
CREATE INDEX idx_order_origin_created
    ON "order" (origin, created_at DESC);

-- -------------------------
-- 8. 주문 항목 (order_item)
-- -------------------------
CREATE TABLE order_item (
    id                       BIGSERIAL     PRIMARY KEY,
    order_id                 BIGINT        NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    product_id               BIGINT        REFERENCES product(id),        -- NULL 이면 Admin 의뢰
    category_id              BIGINT        REFERENCES product_category(id),
    product_name             VARCHAR(255)  NOT NULL,
    product_url              TEXT,
    attributes               JSONB,        -- 카테고리 schema 에 따른 동적 값 (브랜드/모델/도수 등)
    quantity                 INT           NOT NULL DEFAULT 1,
    unit_price_thb           NUMERIC(12, 2),
    purchased_unit_price_krw NUMERIC(12, 0),
    purchased_from           VARCHAR(128),
    purchased_at             TIMESTAMPTZ,
    receipt_image_key        VARCHAR(255),
    request_image_keys       JSONB,        -- 고객 첨부 이미지 S3 key 배열
    out_of_stock_note        TEXT
);

CREATE INDEX idx_order_item_order
    ON order_item (order_id);
CREATE INDEX idx_order_item_category
    ON order_item (category_id);
CREATE INDEX idx_order_item_attributes
    ON order_item USING gin (attributes);  -- JSONB 검색 (브랜드 통계 등)

-- -------------------------
-- 9. 상태 전이 감사 로그 (order_status_log)
-- -------------------------
CREATE TABLE order_status_log (
    id                     BIGSERIAL    PRIMARY KEY,
    order_id               BIGINT       NOT NULL REFERENCES "order"(id),
    dimension              VARCHAR(16)  NOT NULL,   -- 'FULFILLMENT' | 'PAYMENT'
    from_code              VARCHAR(32),             -- NULL 이면 최초 생성
    to_code                VARCHAR(32)  NOT NULL,
    changed_by             BIGINT       REFERENCES app_user(id),      -- NULL 이면 시스템 전이 또는 게스트
    changed_by_customer_id BIGINT       REFERENCES customer(id),      -- 고객 액션이면
    reason_code            VARCHAR(32),             -- common_code: CANCEL_REASON / REJECT_REASON
    note                   TEXT,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_status_log_order_created
    ON order_status_log (order_id, created_at);
