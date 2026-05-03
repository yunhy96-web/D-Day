-- =============================================================
-- V12__seed_line_id_placeholder.sql
-- LINE ID 입력 칸의 placeholder 텍스트를 i18n 화하여 ko/en/th로 표시.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.line_id.placeholder', 'ko', 'username 또는 https://line.me/ti/p/~username', 'admin', true),
    ('field.line_id.placeholder', 'en', 'username or https://line.me/ti/p/~username',  'admin', true),
    ('field.line_id.placeholder', 'th', 'username หรือ https://line.me/ti/p/~username', 'admin', true);
