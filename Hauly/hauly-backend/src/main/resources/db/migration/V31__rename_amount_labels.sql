-- =============================================================
-- V31__rename_amount_labels.sql
-- 워딩 명료화:
--   field.unit_price.label  : 가격/Unit price/ราคาต่อหน่วย → 예상 결제금액 (등록 시 입력)
--   field.paid_amount.label : 결제 금액/Paid amount/ยอดชำระ → 실제 결제금액 (PURCHASED 후 입력)
--   order.col.amount        : 금액/Amount/จำนวนเงิน → 예상 결제금액 (목록 컬럼 헤더)
-- =============================================================

UPDATE i18n_message SET message = '예상 결제금액'
    WHERE message_key = 'field.unit_price.label' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'Expected amount'
    WHERE message_key = 'field.unit_price.label' AND lang_code = 'en';
UPDATE i18n_message SET message = 'ยอดที่คาด'
    WHERE message_key = 'field.unit_price.label' AND lang_code = 'th';

UPDATE i18n_message SET message = '실제 결제금액'
    WHERE message_key = 'field.paid_amount.label' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'Actual amount'
    WHERE message_key = 'field.paid_amount.label' AND lang_code = 'en';
UPDATE i18n_message SET message = 'ยอดชำระจริง'
    WHERE message_key = 'field.paid_amount.label' AND lang_code = 'th';

UPDATE i18n_message SET message = '예상 결제금액'
    WHERE message_key = 'order.col.amount' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'Expected amount'
    WHERE message_key = 'order.col.amount' AND lang_code = 'en';
UPDATE i18n_message SET message = 'ยอดที่คาด'
    WHERE message_key = 'order.col.amount' AND lang_code = 'th';
