-- =============================================================
-- V5__fix_seeds_and_timestamps.sql
-- code-reviewer 자체 피드백 반영:
--   1. is_system 플래그 정정 (운영자가 자유 관리해야 할 그룹은 false)
--   2. updated_at / created_at 컬럼 누락 보강
--   3. CONTACT_LENS schema 에 placeholder_key 추가
--   4. COSMETICS schema 가 lens i18n 키를 재사용하던 것을 자체 namespace 로 교체
--   5. 신규 i18n 키 추가 (placeholder + cosmetics 전용)
-- =============================================================

-- -------------------------------------------------------------
-- 1. is_system 플래그 정정
--    원칙: 애플리케이션 로직이 코드값을 hardcode reference 하는 것만 is_system=true.
--    그 외 (운영자가 추가/수정/삭제 자유) 는 false.
-- -------------------------------------------------------------

-- PAYMENT_METHOD: 전부 사용자 관리 가능
UPDATE common_code
   SET is_system = false
 WHERE group_code = 'PAYMENT_METHOD';

-- COURIER_KR: 전부 사용자 관리 가능 (OTHER 는 이미 false)
UPDATE common_code
   SET is_system = false
 WHERE group_code = 'COURIER_KR'
   AND is_system = true;

-- CANCEL_REASON: OUT_OF_STOCK 은 상태머신에서 hardcoded → 보존, 나머지는 자유 관리
UPDATE common_code
   SET is_system = false
 WHERE group_code = 'CANCEL_REASON'
   AND code <> 'OUT_OF_STOCK';

-- REJECT_REASON: 전부 사용자 관리 가능
UPDATE common_code
   SET is_system = false
 WHERE group_code = 'REJECT_REASON'
   AND is_system = true;

-- -------------------------------------------------------------
-- 2. 누락된 updated_at / created_at 컬럼 보강
-- -------------------------------------------------------------

ALTER TABLE customer
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE product
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE order_item
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- -------------------------------------------------------------
-- 3+4. product_category schema 교체
--      CONTACT_LENS: text 필드에 placeholder_key 추가
--      COSMETICS: lens 키 재사용 → cosmetics 자체 namespace
-- -------------------------------------------------------------

UPDATE product_category
   SET field_schema = '{
        "fields": [
            {
                "key": "brand",
                "label_key": "category.lens.brand",
                "placeholder_key": "category.lens.brand.placeholder",
                "type": "text",
                "required": true
            },
            {
                "key": "model",
                "label_key": "category.lens.model",
                "placeholder_key": "category.lens.model.placeholder",
                "type": "text",
                "required": true
            },
            {
                "key": "color",
                "label_key": "category.lens.color",
                "placeholder_key": "category.lens.color.placeholder",
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
                    {"key":"power",      "label_key":"category.lens.power", "type":"decimal", "step":0.25, "min":-20, "max":6,    "required":true},
                    {"key":"base_curve", "label_key":"category.lens.bc",    "type":"decimal", "step":0.1,  "min":8.0, "max":9.5,  "required":false},
                    {"key":"diameter",   "label_key":"category.lens.dia",   "type":"decimal", "step":0.1,  "min":13.0,"max":15.0, "required":false}
                ]
            },
            {
                "key": "right_eye",
                "label_key": "category.lens.right_eye",
                "type": "group",
                "fields": [
                    {"key":"power",      "label_key":"category.lens.power", "type":"decimal", "step":0.25, "min":-20, "max":6,    "required":true},
                    {"key":"base_curve", "label_key":"category.lens.bc",    "type":"decimal", "step":0.1,  "min":8.0, "max":9.5,  "required":false},
                    {"key":"diameter",   "label_key":"category.lens.dia",   "type":"decimal", "step":0.1,  "min":13.0,"max":15.0, "required":false}
                ]
            }
        ]
    }'::jsonb
 WHERE code = 'CONTACT_LENS';

UPDATE product_category
   SET field_schema = '{
        "fields": [
            {
                "key": "brand",
                "label_key": "category.cosmetics.brand",
                "placeholder_key": "category.cosmetics.brand.placeholder",
                "type": "text",
                "required": true
            },
            {
                "key": "product_name",
                "label_key": "category.cosmetics.product_name",
                "placeholder_key": "category.cosmetics.product_name.placeholder",
                "type": "text",
                "required": true
            },
            {
                "key": "color",
                "label_key": "category.cosmetics.color",
                "placeholder_key": "category.cosmetics.color.placeholder",
                "type": "text",
                "required": false
            },
            {
                "key": "volume",
                "label_key": "category.cosmetics.volume",
                "placeholder_key": "category.cosmetics.volume.placeholder",
                "type": "text",
                "required": false
            }
        ]
    }'::jsonb
 WHERE code = 'COSMETICS';

-- -------------------------------------------------------------
-- 5. 신규 i18n 키 시드
--    (a) CONTACT_LENS placeholder
--    (b) COSMETICS 자체 namespace (brand/color)
--    (c) COSMETICS placeholder 전체
-- -------------------------------------------------------------

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- CONTACT_LENS placeholder
    ('category.lens.brand.placeholder', 'ko', '예: 아큐브, 바슈롬',         'category.lens', true),
    ('category.lens.brand.placeholder', 'en', 'e.g. Acuvue, Bausch+Lomb',   'category.lens', true),
    ('category.lens.brand.placeholder', 'th', 'เช่น Acuvue, Bausch+Lomb',  'category.lens', true),
    ('category.lens.model.placeholder', 'ko', '예: 오아시스 1-Day',          'category.lens', true),
    ('category.lens.model.placeholder', 'en', 'e.g. Oasys 1-Day',           'category.lens', true),
    ('category.lens.model.placeholder', 'th', 'เช่น Oasys 1-Day',           'category.lens', true),
    ('category.lens.color.placeholder', 'ko', '예: 클리어, 브라운',           'category.lens', true),
    ('category.lens.color.placeholder', 'en', 'e.g. Clear, Brown',          'category.lens', true),
    ('category.lens.color.placeholder', 'th', 'เช่น Clear, Brown',           'category.lens', true),

    -- COSMETICS namespace (label) — lens 키 재사용 제거
    ('category.cosmetics.brand', 'ko', '브랜드',  'category.cosmetics', true),
    ('category.cosmetics.brand', 'en', 'Brand',   'category.cosmetics', true),
    ('category.cosmetics.brand', 'th', 'แบรนด์',  'category.cosmetics', true),
    ('category.cosmetics.color', 'ko', '색상',    'category.cosmetics', true),
    ('category.cosmetics.color', 'en', 'Color',   'category.cosmetics', true),
    ('category.cosmetics.color', 'th', 'สี',      'category.cosmetics', true),

    -- COSMETICS placeholder
    ('category.cosmetics.brand.placeholder',        'ko', '예: 이니스프리, 설화수',       'category.cosmetics', true),
    ('category.cosmetics.brand.placeholder',        'en', 'e.g. Innisfree, Sulwhasoo',   'category.cosmetics', true),
    ('category.cosmetics.brand.placeholder',        'th', 'เช่น Innisfree, Sulwhasoo',  'category.cosmetics', true),
    ('category.cosmetics.product_name.placeholder', 'ko', '예: 그린티 세럼',              'category.cosmetics', true),
    ('category.cosmetics.product_name.placeholder', 'en', 'e.g. Green Tea Serum',        'category.cosmetics', true),
    ('category.cosmetics.product_name.placeholder', 'th', 'เช่น Green Tea Serum',       'category.cosmetics', true),
    ('category.cosmetics.color.placeholder',        'ko', '예: #21 라이트',                'category.cosmetics', true),
    ('category.cosmetics.color.placeholder',        'en', 'e.g. #21 Light',              'category.cosmetics', true),
    ('category.cosmetics.color.placeholder',        'th', 'เช่น #21 Light',              'category.cosmetics', true),
    ('category.cosmetics.volume.placeholder',       'ko', '예: 50ml, 100g',                'category.cosmetics', true),
    ('category.cosmetics.volume.placeholder',       'en', 'e.g. 50ml, 100g',              'category.cosmetics', true),
    ('category.cosmetics.volume.placeholder',       'th', 'เช่น 50ml, 100g',              'category.cosmetics', true)
;
