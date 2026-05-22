-- =============================================================
-- FlashArena :: V6 - Outbox 를 payment → order 스키마로 이동 (마이그레이션)
-- -------------------------------------------------------------
-- 이유: Transactional Outbox 는 "이벤트 생산자"가 소유해야 비즈니스 데이터(orders)와
--       같은 로컬 트랜잭션으로 원자 커밋된다. "주문완료" 이벤트의 생산자는 order 도메인이므로
--       outbox 는 order 스키마에 있어야 교차 스키마 트랜잭션 없이 유실 0% 가 성립한다.
--
-- 멱등(idempotent) 설계:
--   - 신규 DB: V3 가 이미 "order".outbox 를 만들었으므로 아래 CREATE 는 무동작(IF NOT EXISTS).
--   - 기존 DB: payment.outbox(비어있음)를 제거하고 "order".outbox 를 생성한다.
-- =============================================================

-- 1) order 스키마에 outbox 보장 (없으면 생성)
CREATE TABLE IF NOT EXISTS "order".outbox (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50)  NOT NULL DEFAULT 'ORDER',
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'READY',
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_ready
    ON "order".outbox (created_at)
    WHERE status = 'READY';

-- 2) payment 스키마에서 outbox 제거 (payment 는 inbox=payment_history 만 보유).
--    라이브 payment.outbox 는 한 번도 사용되지 않아 비어있으므로 데이터 이관 불필요.
DROP TABLE IF EXISTS payment.outbox;
