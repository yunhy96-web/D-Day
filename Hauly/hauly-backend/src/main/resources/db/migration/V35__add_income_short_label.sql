-- =============================================================
-- V35__add_income_short_label.sql
-- 주문 목록 AMOUNT 컬럼 안 "수익" 행 짧은 prefix.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.income.short', 'ko', '수익',    'admin', true),
    ('field.income.short', 'en', 'Income',  'admin', true),
    ('field.income.short', 'th', 'กำไร',     'admin', true);
