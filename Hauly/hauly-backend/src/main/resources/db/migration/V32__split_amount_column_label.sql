-- =============================================================
-- V32__split_amount_column_label.sql
-- 주문 목록 컬럼 헤더는 "결제금액" 그룹명으로 되돌리고,
-- 셀 안에서 "예상" / "실제" 짧은 prefix로 분리 표시 위해 새 키 2개 추가.
-- =============================================================

UPDATE i18n_message SET message = '결제금액'
    WHERE message_key = 'order.col.amount' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'Amount'
    WHERE message_key = 'order.col.amount' AND lang_code = 'en';
UPDATE i18n_message SET message = 'ยอดชำระ'
    WHERE message_key = 'order.col.amount' AND lang_code = 'th';

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.expected.short', 'ko', '예상',     'admin', true),
    ('field.expected.short', 'en', 'Expected', 'admin', true),
    ('field.expected.short', 'th', 'คาดการณ์',  'admin', true),

    ('field.actual.short',   'ko', '실제',     'admin', true),
    ('field.actual.short',   'en', 'Actual',   'admin', true),
    ('field.actual.short',   'th', 'จริง',     'admin', true);
