-- =============================================================
-- V4__seed_categories.sql  — 시드 상품 카테고리
-- field_schema: JSON Schema 형태의 동적 필드 정의
-- =============================================================

-- -------------------------
-- 1. GENERAL — 기본 카테고리 (빈 스키마)
-- -------------------------
INSERT INTO product_category (code, name_key, field_schema, is_active, sort_order) VALUES (
    'GENERAL',
    'category.general.name',
    '{"fields": []}'::jsonb,
    true,
    10
);

-- -------------------------
-- 2. CONTACT_LENS — 콘택트렌즈 전용 스키마
-- -------------------------
INSERT INTO product_category (code, name_key, field_schema, is_active, sort_order) VALUES (
    'CONTACT_LENS',
    'category.contact_lens.name',
    '{
        "fields": [
            {
                "key": "brand",
                "label_key": "category.lens.brand",
                "type": "text",
                "required": true
            },
            {
                "key": "model",
                "label_key": "category.lens.model",
                "type": "text",
                "required": true
            },
            {
                "key": "color",
                "label_key": "category.lens.color",
                "type": "text",
                "required": false
            },
            {
                "key": "wear_cycle",
                "label_key": "category.lens.wear_cycle",
                "type": "select",
                "options_code": "LENS_WEAR_CYCLE",
                "required": true
            },
            {
                "key": "left_eye",
                "label_key": "category.lens.left_eye",
                "type": "group",
                "fields": [
                    {
                        "key": "power",
                        "label_key": "category.lens.power",
                        "type": "decimal",
                        "step": 0.25,
                        "min": -20,
                        "max": 6,
                        "required": true
                    },
                    {
                        "key": "base_curve",
                        "label_key": "category.lens.bc",
                        "type": "decimal",
                        "step": 0.1,
                        "min": 8.0,
                        "max": 9.5,
                        "required": false
                    },
                    {
                        "key": "diameter",
                        "label_key": "category.lens.dia",
                        "type": "decimal",
                        "step": 0.1,
                        "min": 13.0,
                        "max": 15.0,
                        "required": false
                    }
                ]
            },
            {
                "key": "right_eye",
                "label_key": "category.lens.right_eye",
                "type": "group",
                "fields": [
                    {
                        "key": "power",
                        "label_key": "category.lens.power",
                        "type": "decimal",
                        "step": 0.25,
                        "min": -20,
                        "max": 6,
                        "required": true
                    },
                    {
                        "key": "base_curve",
                        "label_key": "category.lens.bc",
                        "type": "decimal",
                        "step": 0.1,
                        "min": 8.0,
                        "max": 9.5,
                        "required": false
                    },
                    {
                        "key": "diameter",
                        "label_key": "category.lens.dia",
                        "type": "decimal",
                        "step": 0.1,
                        "min": 13.0,
                        "max": 15.0,
                        "required": false
                    }
                ]
            }
        ]
    }'::jsonb,
    true,
    20
);

-- -------------------------
-- 3. COSMETICS — 화장품 (골격, 실제 필드는 의뢰 시 보강)
-- -------------------------
INSERT INTO product_category (code, name_key, field_schema, is_active, sort_order) VALUES (
    'COSMETICS',
    'category.cosmetics.name',
    '{
        "fields": [
            {
                "key": "brand",
                "label_key": "category.lens.brand",
                "type": "text",
                "required": true
            },
            {
                "key": "product_name",
                "label_key": "category.cosmetics.product_name",
                "type": "text",
                "required": true
            },
            {
                "key": "color",
                "label_key": "category.lens.color",
                "type": "text",
                "required": false
            },
            {
                "key": "volume",
                "label_key": "category.cosmetics.volume",
                "type": "text",
                "required": false
            }
        ]
    }'::jsonb,
    true,
    30
);

-- -------------------------
-- COSMETICS 전용 i18n 키 추가 (V3 에서 누락된 카테고리별 키)
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('category.general.name',         'ko', '일반',          'category',           true),
    ('category.general.name',         'en', 'General',       'category',           true),
    ('category.general.name',         'th', 'ทั่วไป',         'category',           true),
    ('category.contact_lens.name',    'ko', '콘택트렌즈',     'category',           true),
    ('category.contact_lens.name',    'en', 'Contact Lens',  'category',           true),
    ('category.contact_lens.name',    'th', 'คอนแทคเลนส์',   'category',           true),
    ('category.cosmetics.name',       'ko', '화장품',         'category',           true),
    ('category.cosmetics.name',       'en', 'Cosmetics',     'category',           true),
    ('category.cosmetics.name',       'th', 'เครื่องสำอาง',   'category',           true),
    ('category.cosmetics.product_name','ko','제품명',          'category.cosmetics', true),
    ('category.cosmetics.product_name','en','Product Name',   'category.cosmetics', true),
    ('category.cosmetics.product_name','th','ชื่อสินค้า',      'category.cosmetics', true),
    ('category.cosmetics.volume',     'ko', '용량',           'category.cosmetics', true),
    ('category.cosmetics.volume',     'en', 'Volume',        'category.cosmetics', true),
    ('category.cosmetics.volume',     'th', 'ปริมาณ',         'category.cosmetics', true)
;
