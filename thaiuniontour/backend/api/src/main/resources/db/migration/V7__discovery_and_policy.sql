-- V7: 연관상품 + 취소/환불 정책 + 예약 확정 라이프사이클(입금기한/취소사유) + 환불

-- ── 연관상품: admin 수동 큐레이션 (자동 fallback은 동일 카테고리/지역 쿼리) ──
CREATE TABLE catalog.product_related (
    product_id          UUID NOT NULL REFERENCES catalog.product(id) ON DELETE CASCADE,
    related_product_id  UUID NOT NULL REFERENCES catalog.product(id) ON DELETE CASCADE,
    sort                INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, related_product_id),
    CHECK (product_id <> related_product_id)
);
CREATE INDEX idx_product_related ON catalog.product_related(product_id, sort);

-- ── 취소/환불 정책: 재사용 템플릿. admin이 정의, 상품이 참조 ──
CREATE TABLE catalog.cancellation_policy (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    use_yn      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 규칙: 출발 D-day 기준 환불율(%). 적용 = (실제 출발잔여일 >= min_days_before) 중 min_days_before 최댓값.
CREATE TABLE catalog.cancellation_policy_rule (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id        UUID NOT NULL REFERENCES catalog.cancellation_policy(id) ON DELETE CASCADE,
    min_days_before  INT  NOT NULL CHECK (min_days_before >= 0),
    refund_rate      INT  NOT NULL CHECK (refund_rate BETWEEN 0 AND 100),
    UNIQUE (policy_id, min_days_before)
);
CREATE INDEX idx_cxl_rule_policy ON catalog.cancellation_policy_rule(policy_id, min_days_before DESC);

-- 상품 → 정책 참조(catalog 내부 FK 허용). NULL = 정책 미지정(취소불가 또는 기본정책 적용은 앱 정책)
ALTER TABLE catalog.product
    ADD COLUMN cancellation_policy_id UUID REFERENCES catalog.cancellation_policy(id);

-- ── 예약 확정 라이프사이클: 입금기한 + 취소사유 ──
-- PENDING은 '임시 확보'. payment_deadline 내 미입금 시 만료 처리(EXPIRED) → 재고 복원.
ALTER TABLE reservation.reservation
    ADD COLUMN payment_deadline TIMESTAMPTZ,
    ADD COLUMN cancel_reason    VARCHAR(20)
        CHECK (cancel_reason IS NULL OR cancel_reason IN ('CUSTOMER','ADMIN','EXPIRED'));
-- 만료 스위퍼가 PENDING+기한초과 건을 빠르게 찾기 위한 부분 인덱스
CREATE INDEX idx_reservation_pending_deadline
    ON reservation.reservation(payment_deadline)
    WHERE status = 'PENDING';

-- ── 환불(payment 소유): 정책 기반 산정액. reservation당 1건(멱등) ──
CREATE TABLE payment.refund (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id  UUID NOT NULL,                 -- 논리참조
    amount          BIGINT NOT NULL DEFAULT 0,     -- 환불액(정책 적용 후)
    refund_rate     INT  NOT NULL DEFAULT 0,       -- 적용 환불율(%)
    reason          VARCHAR(20) NOT NULL CHECK (reason IN ('CUSTOMER','ADMIN','EXPIRED')),
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                    CHECK (status IN ('REQUESTED','COMPLETED')),  -- 계좌이체는 수동 송금 → COMPLETED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_refund_reservation ON payment.refund(reservation_id);

-- ── 시드: 표준 취소정책 예시 ──
WITH p AS (
    INSERT INTO catalog.cancellation_policy(name, description)
    VALUES ('표준 취소정책', '출발 7일 전 100% / 3~6일 50% / 2일 이내 환불불가')
    RETURNING id
)
INSERT INTO catalog.cancellation_policy_rule(policy_id, min_days_before, refund_rate)
SELECT p.id, v.d, v.r FROM p, (VALUES (7,100),(3,50),(0,0)) AS v(d, r);
