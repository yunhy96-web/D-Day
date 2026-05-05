-- =============================================================
-- V25__shipping_address_template.sql
-- 자주 쓰는 배송지를 별칭으로 저장해 주문 등록 시 불러오는 템플릿.
-- 운영자가 3명뿐이라 공유 풀로 운영 (created_by 만 기록).
-- order에 shipping_address_label 컬럼을 함께 추가 — 템플릿이 삭제되어도
-- 주문 목록에서 어떤 배송지로 나갔는지 식별 가능하도록 라벨 텍스트를 직접 저장.
-- =============================================================

CREATE TABLE shipping_address_template (
    id              BIGSERIAL PRIMARY KEY,
    label           VARCHAR(64)  NOT NULL,                        -- 별칭 (예: "방콕 김선생님")
    recipient_name  VARCHAR(64),
    recipient_phone VARCHAR(32),
    postal_code     VARCHAR(16),
    address_line    TEXT,
    country         VARCHAR(2),                                   -- ISO 3166-1 alpha-2
    created_by      BIGINT REFERENCES app_user(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 같은 라벨 중복 방지 (대소문자 구분 없이)
CREATE UNIQUE INDEX uk_shipping_address_template_label
    ON shipping_address_template (lower(label));

ALTER TABLE "order"
    ADD COLUMN shipping_address_label VARCHAR(64);

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.shipping_label.label',  'ko', '배송지 별칭',          'admin', true),
    ('field.shipping_label.label',  'en', 'Address label',        'admin', true),
    ('field.shipping_label.label',  'th', 'ชื่อกำกับที่อยู่',         'admin', true),

    ('shipping.template.load',       'ko', '저장된 배송지 불러오기', 'admin', true),
    ('shipping.template.load',       'en', 'Load saved address',    'admin', true),
    ('shipping.template.load',       'th', 'โหลดที่อยู่ที่บันทึกไว้', 'admin', true),

    ('shipping.template.save',       'ko', '이 배송지 저장',         'admin', true),
    ('shipping.template.save',       'en', 'Save this address',     'admin', true),
    ('shipping.template.save',       'th', 'บันทึกที่อยู่นี้',         'admin', true),

    ('shipping.template.save.title', 'ko', '배송지 저장',            'admin', true),
    ('shipping.template.save.title', 'en', 'Save address',          'admin', true),
    ('shipping.template.save.title', 'th', 'บันทึกที่อยู่',           'admin', true),

    ('shipping.template.label_help', 'ko', '구분하기 쉬운 별칭을 입력하세요. 예: 방콕 김선생님',
                                    'admin', true),
    ('shipping.template.label_help', 'en', 'Enter a recognizable nickname. e.g., "Bangkok John"',
                                    'admin', true),
    ('shipping.template.label_help', 'th', 'ป้อนชื่อกำกับที่จดจำได้ง่าย เช่น "John กรุงเทพ"',
                                    'admin', true),

    ('shipping.template.empty',      'ko', '저장된 배송지가 없습니다.', 'admin', true),
    ('shipping.template.empty',      'en', 'No saved addresses yet.',   'admin', true),
    ('shipping.template.empty',      'th', 'ยังไม่มีที่อยู่ที่บันทึกไว้',   'admin', true),

    ('order.col.shipping_to',        'ko', '배송지',                'admin', true),
    ('order.col.shipping_to',        'en', 'Ship to',               'admin', true),
    ('order.col.shipping_to',        'th', 'จัดส่งถึง',              'admin', true);
