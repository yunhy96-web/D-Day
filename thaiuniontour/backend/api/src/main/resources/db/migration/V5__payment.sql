-- V5: payment (소비자 멱등 Inbox)
-- 결제 이력 = Inbox. reservation_id UNIQUE 가 중복결제 최종 방어선(exactly-once 효과).
CREATE TABLE payment.payment_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id  UUID NOT NULL,                  -- 멱등키 (reservation 논리참조)
    member_id       UUID,                           -- nullable=게스트
    method          VARCHAR(50) NOT NULL,
    amount          BIGINT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PAID'
                    CHECK (status IN ('PAID','CANCELLED','REFUNDED')),
    paid_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payment_reservation ON payment.payment_history(reservation_id);
