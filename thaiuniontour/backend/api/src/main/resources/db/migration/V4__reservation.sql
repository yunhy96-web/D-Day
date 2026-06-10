-- V4: reservation (예약 + Transactional Outbox)
CREATE TABLE reservation.reservation (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id            UUID,                       -- nullable=게스트 (member 논리참조)
    product_id           UUID NOT NULL,              -- catalog 논리참조
    product_schedule_id  UUID NOT NULL,              -- catalog 논리참조(차감 대상)
    sale_date            DATE NOT NULL,
    quantity             INT  NOT NULL DEFAULT 1 CHECK (quantity > 0),
    amount               BIGINT NOT NULL DEFAULT 0,  -- KRW 결제 총액
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','CONFIRMED','CANCELLED')),
    payment_method       VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
    reserver_name        VARCHAR(100) NOT NULL,
    reserver_email       VARCHAR(255) NOT NULL,
    reserver_phone       VARCHAR(30)  NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at         TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ
    -- 좌석 hold seam: 필요시 status 'HELD' + held_until TIMESTAMPTZ 추가.
);
CREATE INDEX idx_reservation_member   ON reservation.reservation(member_id);
CREATE INDEX idx_reservation_schedule ON reservation.reservation(product_schedule_id);
CREATE INDEX idx_reservation_status   ON reservation.reservation(status);

-- Transactional Outbox (생산자=reservation 소유). 예약 INSERT와 동일 로컬 TX로 원자 커밋.
CREATE TABLE reservation.outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL DEFAULT 'RESERVATION',
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         JSONB NOT NULL,                  -- {version, reservationId, amount, ...}
    status          VARCHAR(20) NOT NULL DEFAULT 'READY'
                    CHECK (status IN ('READY','PUBLISHED','FAILED')),
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);
-- 릴레이어가 READY만 빠르게 폴링 (부분 인덱스)
CREATE INDEX idx_outbox_ready ON reservation.outbox(created_at) WHERE status = 'READY';
