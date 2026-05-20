-- =============================================================
-- V34__add_reset_button_labels.sql
-- 재무 정보 모달의 초기화 관련 라벨.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('btn.reset_all', 'ko', '모두 초기화',  'admin', true),
    ('btn.reset_all', 'en', 'Reset all',    'admin', true),
    ('btn.reset_all', 'th', 'ล้างทั้งหมด',    'admin', true),

    ('aria.field_reset', 'ko', '필드 초기화', 'admin', true),
    ('aria.field_reset', 'en', 'Reset field', 'admin', true),
    ('aria.field_reset', 'th', 'ล้างค่า',     'admin', true);
