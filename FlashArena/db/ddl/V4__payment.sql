-- =============================================================
-- FlashArena :: V4 - payment 스키마 (소비자 멱등성 = Inbox)
-- -------------------------------------------------------------
-- payment_history : 소비자 멱등성 보장 (order_id UNIQUE)
--
-- ⚠️ Outbox 는 payment 가 아니라 "이벤트 생산자"인 order 스키마에 있다 (V3 참조).
--    payment 는 이벤트를 "수신"만 하므로 outbox 가 아니라 inbox(처리 이력)가 필요하다.
--      - 유실 방지(at-least-once 발행)  → Outbox  → 생산자(order) 소유
--      - 중복 방지(exactly-once 효과)   → Inbox   → 소비자(payment) 소유 ← 이 테이블
-- =============================================================

-- 소비자 멱등성 이력 (Inbox)
CREATE TABLE IF NOT EXISTS payment.payment_history (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL,            -- "order".orders.id 참조 (타입만, FK 없음)
    amount      BIGINT       NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PAID',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ⭐ 멱등성 키: 같은 order 는 한 번만 결제 처리 (DB 레벨 최종 방어선).
--    exists() 선체크는 빠른 경로일 뿐, 동시 중복 도착 시 진짜로 막는 건 이 UNIQUE 다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_history_order_id
    ON payment.payment_history (order_id);

COMMENT ON COLUMN payment.payment_history.order_id IS '멱등성 키. UNIQUE 로 중복 결제 차단 (DuplicateKey → 이미 처리됨)';
