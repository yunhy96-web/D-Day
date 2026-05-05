-- =============================================================
-- V23__widen_text_fields_and_paid_amount.sql
-- 사용자 요청에 따라 길이 제한을 500자로 확장 (라인ID, 송장번호).
-- 주문 PURCHASED 시 실제 결제 금액을 주문 자체에도 기록 (deposit_transaction과 별도로
-- 상세 페이지에서 빠르게 노출하기 위함).
-- =============================================================

-- 길이 제한 확장 — 64 → 500
ALTER TABLE customer
    ALTER COLUMN line_id TYPE VARCHAR(500);

ALTER TABLE "order"
    ALTER COLUMN korean_tracking_no TYPE VARCHAR(500);

-- 결제 금액 (KRW). NULL 허용 — PURCHASED 이전에는 비어 있음.
ALTER TABLE "order"
    ADD COLUMN paid_amount_krw NUMERIC(15, 2);

-- 새 i18n 라벨 (목록 컬럼 + 상세 결제금액)
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.col.product',       'ko', '상품',       'admin', true),
    ('order.col.product',       'en', 'Product',    'admin', true),
    ('order.col.product',       'th', 'สินค้า',     'admin', true),

    ('order.col.tracking',      'ko', '배송',       'admin', true),
    ('order.col.tracking',      'en', 'Shipping',   'admin', true),
    ('order.col.tracking',      'th', 'จัดส่ง',     'admin', true),

    ('order.col.items.more',    'ko', '개 더',      'admin', true),
    ('order.col.items.more',    'en', 'more',       'admin', true),
    ('order.col.items.more',    'th', 'รายการเพิ่ม', 'admin', true),

    ('field.paid_amount.label', 'ko', '결제 금액',           'admin', true),
    ('field.paid_amount.label', 'en', 'Paid amount',         'admin', true),
    ('field.paid_amount.label', 'th', 'ยอดชำระ',             'admin', true);
