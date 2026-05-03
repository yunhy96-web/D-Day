-- =============================================================
-- V16__add_shipping_address.sql
-- 배송지 정보 5컬럼을 order 테이블에 추가. 모두 nullable (선택 입력).
-- 동남아 고객의 다양한 주소 형태를 자유롭게 받기 위해 address_line은 TEXT.
-- country는 ISO 3166-1 alpha-2 (TH/KR/US/...).
-- =============================================================

ALTER TABLE "order"
    ADD COLUMN recipient_name  VARCHAR(64),
    ADD COLUMN recipient_phone VARCHAR(32),
    ADD COLUMN postal_code     VARCHAR(16),
    ADD COLUMN address_line    TEXT,
    ADD COLUMN country         VARCHAR(2);

-- ----- i18n 라벨 (ko/en/th) -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.create.shipping_address',  'ko', '배송지',         'admin', true),
    ('order.create.shipping_address',  'en', 'Shipping Address', 'admin', true),
    ('order.create.shipping_address',  'th', 'ที่อยู่จัดส่ง',     'admin', true),

    ('order.detail.shipping_address',  'ko', '배송지',         'admin', true),
    ('order.detail.shipping_address',  'en', 'Shipping Address', 'admin', true),
    ('order.detail.shipping_address',  'th', 'ที่อยู่จัดส่ง',     'admin', true),

    ('field.recipient_name.label',     'ko', '수취인 이름',     'admin', true),
    ('field.recipient_name.label',     'en', 'Recipient Name',  'admin', true),
    ('field.recipient_name.label',     'th', 'ชื่อผู้รับ',         'admin', true),

    ('field.recipient_phone.label',    'ko', '수취인 전화',     'admin', true),
    ('field.recipient_phone.label',    'en', 'Recipient Phone', 'admin', true),
    ('field.recipient_phone.label',    'th', 'เบอร์โทรผู้รับ',     'admin', true),

    ('field.postal_code.label',        'ko', '우편번호',        'admin', true),
    ('field.postal_code.label',        'en', 'Postal Code',     'admin', true),
    ('field.postal_code.label',        'th', 'รหัสไปรษณีย์',     'admin', true),

    ('field.address_line.label',       'ko', '주소',           'admin', true),
    ('field.address_line.label',       'en', 'Address',         'admin', true),
    ('field.address_line.label',       'th', 'ที่อยู่',           'admin', true),

    ('field.country.label',            'ko', '국가',           'admin', true),
    ('field.country.label',            'en', 'Country',         'admin', true),
    ('field.country.label',            'th', 'ประเทศ',          'admin', true);
