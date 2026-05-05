-- =============================================================
-- V28__add_price_required_errors.sql
-- 주문 등록 시 단가/통화 필수 검증 에러 메시지.
-- 프론트 zod superRefine + 백엔드 @NotNull / @Pattern 양쪽에서 사용.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('msg.error.price_required',    'ko', '단가를 입력해주세요.',                'admin', true),
    ('msg.error.price_required',    'en', 'Please enter a unit price.',          'admin', true),
    ('msg.error.price_required',    'th', 'กรุณาระบุราคาต่อหน่วย',                 'admin', true),

    ('msg.error.currency_required', 'ko', '통화를 선택해주세요.',                'admin', true),
    ('msg.error.currency_required', 'en', 'Please select a currency.',           'admin', true),
    ('msg.error.currency_required', 'th', 'กรุณาเลือกสกุลเงิน',                    'admin', true);
