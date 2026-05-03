-- =============================================================
-- V10__add_unit_price_currency_and_tracking_labels.sql
-- 1. order_item 에 통화·금액 컬럼 추가 (KRW / THB / USD 모두 지원)
-- 2. CURRENCY common_code 그룹 시드
-- 3. 신규 UI 라벨 (price, currency, tracking, dashboard 요약) 시드
-- =============================================================

-- ----- 1. order_item 가격/통화 -----
ALTER TABLE order_item
    ADD COLUMN unit_price_amount   NUMERIC(12, 2),
    ADD COLUMN unit_price_currency VARCHAR(8);

ALTER TABLE order_item
    ADD CONSTRAINT chk_order_item_currency
        CHECK (unit_price_currency IS NULL OR unit_price_currency IN ('KRW', 'THB', 'USD'));

-- 합산 쿼리 (currency별 합계) 가속용 — currency가 비어있는 행은 제외
CREATE INDEX idx_order_item_price_currency
    ON order_item (unit_price_currency, order_id)
    WHERE unit_price_amount IS NOT NULL;

-- ----- 2. CURRENCY common_code 그룹 + 코드 -----
INSERT INTO common_code_group (group_code, group_name_ko, group_name_en, description, is_system) VALUES
    ('CURRENCY', '통화', 'Currency', '주문 가격 입력 시 사용 가능한 통화', true);

INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('CURRENCY', 'KRW', '원',  'KRW (₩)', 'KRW', 10, true),
    ('CURRENCY', 'THB', '바트', 'THB (฿)', 'THB', 20, true),
    ('CURRENCY', 'USD', '달러', 'USD ($)', 'USD', 30, true);

-- ----- 3. UI 라벨 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- 주문 등록 폼
    ('field.unit_price.label',     'ko', '가격',                 'admin', true),
    ('field.unit_price.label',     'en', 'Unit price',           'admin', true),
    ('field.unit_price.label',     'th', 'ราคาต่อหน่วย',           'admin', true),
    ('field.currency.label',       'ko', '통화',                 'admin', true),
    ('field.currency.label',       'en', 'Currency',             'admin', true),
    ('field.currency.label',       'th', 'สกุลเงิน',              'admin', true),
    ('field.tracking_no.label',    'ko', '한국 송장번호',          'admin', true),
    ('field.tracking_no.label',    'en', 'KR tracking no.',       'admin', true),
    ('field.tracking_no.label',    'th', 'หมายเลขพัสดุเกาหลี',     'admin', true),
    ('field.courier.label',        'ko', '택배사',                'admin', true),
    ('field.courier.label',        'en', 'Courier',              'admin', true),
    ('field.courier.label',        'th', 'บริษัทขนส่ง',           'admin', true),
    ('order.create.tracking',      'ko', '배송 정보',             'admin', true),
    ('order.create.tracking',      'en', 'Shipping',             'admin', true),
    ('order.create.tracking',      'th', 'การจัดส่ง',             'admin', true),
    -- 목록/상세 컬럼
    ('order.col.amount',           'ko', '금액',                  'admin', true),
    ('order.col.amount',           'en', 'Amount',               'admin', true),
    ('order.col.amount',           'th', 'จำนวนเงิน',             'admin', true),
    ('order.detail.tracking',      'ko', '배송 정보',             'admin', true),
    ('order.detail.tracking',      'en', 'Shipping',             'admin', true),
    ('order.detail.tracking',      'th', 'การจัดส่ง',             'admin', true),
    -- 대시보드
    ('dashboard.title',            'ko', '대시보드',              'admin', true),
    ('dashboard.title',            'en', 'Dashboard',            'admin', true),
    ('dashboard.title',            'th', 'แดชบอร์ด',              'admin', true),
    ('dashboard.totals.title',     'ko', '통화별 누적 금액',        'admin', true),
    ('dashboard.totals.title',     'en', 'Totals by currency',    'admin', true),
    ('dashboard.totals.title',     'th', 'รวมตามสกุลเงิน',         'admin', true),
    ('dashboard.totals.empty',     'ko', '아직 입력된 금액이 없습니다.', 'admin', true),
    ('dashboard.totals.empty',     'en', 'No amounts recorded yet.',   'admin', true),
    ('dashboard.totals.empty',     'th', 'ยังไม่มีจำนวนเงินบันทึก',     'admin', true),
    ('dashboard.orders.title',     'ko', '총 주문 수',            'admin', true),
    ('dashboard.orders.title',     'en', 'Total orders',         'admin', true),
    ('dashboard.orders.title',     'th', 'จำนวนคำสั่งซื้อทั้งหมด',  'admin', true),
    ('dashboard.by_status.title',  'ko', '상태별 주문',            'admin', true),
    ('dashboard.by_status.title',  'en', 'Orders by status',     'admin', true),
    ('dashboard.by_status.title',  'th', 'คำสั่งซื้อตามสถานะ',     'admin', true);
