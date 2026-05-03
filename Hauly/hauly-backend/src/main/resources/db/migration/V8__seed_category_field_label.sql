-- =============================================================
-- V8__seed_category_field_label.sql
-- 주문 등록/상세에 카테고리 셀렉트가 추가되면서 필요한 라벨 추가.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.category.label', 'ko', '카테고리',  'admin', true),
    ('field.category.label', 'en', 'Category',  'admin', true),
    ('field.category.label', 'th', 'หมวดหมู่',  'admin', true);
