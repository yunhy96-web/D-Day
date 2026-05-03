-- =============================================================
-- V11__update_line_id_label_to_include_link.sql
-- LINE ID 입력 칸이 ID 뿐 아니라 링크(https://line.me/ti/p/~...)도 받도록
-- 변경됨에 따라, 라벨도 "LINE ID/LINK"로 변경하여 입력 가능 형식을 명시.
-- =============================================================

UPDATE i18n_message SET message = 'LINE ID/LINK' WHERE message_key = 'field.line_id.label' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'LINE ID/LINK' WHERE message_key = 'field.line_id.label' AND lang_code = 'en';
UPDATE i18n_message SET message = 'LINE ID/LINK' WHERE message_key = 'field.line_id.label' AND lang_code = 'th';
