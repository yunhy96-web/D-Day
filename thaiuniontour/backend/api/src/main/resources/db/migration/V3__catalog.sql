-- V3: catalog (분류·상품·재고·검색)

-- 카테고리: self-ref 트리 + type. 테마(주 네비 3단계)/지역(패싯) 동일 구조.
CREATE TABLE catalog.category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES catalog.category(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL CHECK (type IN ('THEME','DESTINATION')),
    level       INT  NOT NULL DEFAULT 1 CHECK (level BETWEEN 1 AND 3),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    sort        INT  NOT NULL DEFAULT 0,
    use_yn      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (type, slug)
);
CREATE INDEX idx_category_parent ON catalog.category(parent_id);
CREATE INDEX idx_category_type   ON catalog.category(type, sort);

-- 상품: stock_type으로 재고형 구분. search_tsv는 generated column(GIN).
CREATE TABLE catalog.product (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    summary       VARCHAR(500),
    description   TEXT,                              -- 상세페이지 HTML(리치에디터)
    stock_type    VARCHAR(20) NOT NULL DEFAULT 'LIMITED'
                  CHECK (stock_type IN ('LIMITED','UNLIMITED')),
    base_price    BIGINT NOT NULL DEFAULT 0,         -- KRW
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT','ON_SALE','SOLD_OUT','CLOSED')),
    thumbnail_url VARCHAR(500),
    search_tsv    tsvector GENERATED ALWAYS AS (
                      to_tsvector('simple',
                        coalesce(name,'') || ' ' || coalesce(summary,''))
                  ) STORED,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_product_search    ON catalog.product USING GIN (search_tsv);
CREATE INDEX idx_product_name_trgm ON catalog.product USING GIN (name gin_trgm_ops);
CREATE INDEX idx_product_status    ON catalog.product(status);

-- 상품-카테고리 N:M (테마/지역 동시 태깅). primary=브레드크럼/대표분류.
CREATE TABLE catalog.product_category (
    product_id  UUID NOT NULL REFERENCES catalog.product(id)  ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES catalog.category(id) ON DELETE CASCADE,
    is_primary  BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (product_id, category_id)
);
CREATE INDEX idx_prodcat_category ON catalog.product_category(category_id);

-- 날짜별 재고/가격 — 동시성(선착순)의 핵심 자원. stock NULL=무제한.
CREATE TABLE catalog.product_schedule (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES catalog.product(id) ON DELETE CASCADE,
    sale_date   DATE NOT NULL,
    stock       INT,                                 -- NULL=무제한, 정수=한정
    price       BIGINT NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                CHECK (status IN ('OPEN','SOLD_OUT','CLOSED')),
    version     BIGINT NOT NULL DEFAULT 0,           -- 낙관적 락 seam
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, sale_date),
    CHECK (stock IS NULL OR stock >= 0)
);
CREATE INDEX idx_schedule_product_date ON catalog.product_schedule(product_id, sale_date);

-- 상세페이지 이미지
CREATE TABLE catalog.product_image (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES catalog.product(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    sort        INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_product_image_product ON catalog.product_image(product_id);
