-- =============================================================
-- V29__add_order_financials.sql
-- 주문별 순수익 계산을 위한 재무 필드 추가:
--   - customer_revenue_amount/currency  : 고객이 송금한 금액 (KRW or THB)
--   - logistics_kr_to_th_amount/currency: 한→태 국제 물류비
--   - logistics_th_domestic_amount/currency: 태국 내 배송비
--   - krw_per_thb: 주문별 환율 (1 THB = N KRW). THB 값 환산용.
-- 모두 nullable — 단계적으로 채울 수 있음 (PURCHASED 전이라도 입력 가능).
-- 통화 enum: KRW / THB만 허용.
-- =============================================================

ALTER TABLE "order"
    ADD COLUMN customer_revenue_amount       NUMERIC(12, 2),
    ADD COLUMN customer_revenue_currency     VARCHAR(3),
    ADD COLUMN logistics_kr_to_th_amount     NUMERIC(12, 2),
    ADD COLUMN logistics_kr_to_th_currency   VARCHAR(3),
    ADD COLUMN logistics_th_domestic_amount  NUMERIC(12, 2),
    ADD COLUMN logistics_th_domestic_currency VARCHAR(3),
    ADD COLUMN krw_per_thb                   NUMERIC(10, 4);

ALTER TABLE "order"
    ADD CONSTRAINT chk_order_revenue_currency
    CHECK (customer_revenue_currency IS NULL
           OR customer_revenue_currency IN ('KRW','THB')),
    ADD CONSTRAINT chk_order_logistics_kr_currency
    CHECK (logistics_kr_to_th_currency IS NULL
           OR logistics_kr_to_th_currency IN ('KRW','THB')),
    ADD CONSTRAINT chk_order_logistics_th_currency
    CHECK (logistics_th_domestic_currency IS NULL
           OR logistics_th_domestic_currency IN ('KRW','THB'));

-- i18n 라벨/모달 메시지
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.customer_revenue.label',   'ko', '고객 입금액',          'admin', true),
    ('field.customer_revenue.label',   'en', 'Customer payment',     'admin', true),
    ('field.customer_revenue.label',   'th', 'ยอดที่ลูกค้าโอน',         'admin', true),

    ('field.logistics_kr_th.label',    'ko', '한→태 물류비',          'admin', true),
    ('field.logistics_kr_th.label',    'en', 'KR→TH logistics fee',  'admin', true),
    ('field.logistics_kr_th.label',    'th', 'ค่าขนส่ง KR→TH',         'admin', true),

    ('field.logistics_th_dom.label',   'ko', '태국 내 배송비',        'admin', true),
    ('field.logistics_th_dom.label',   'en', 'TH domestic delivery', 'admin', true),
    ('field.logistics_th_dom.label',   'th', 'ค่าจัดส่งภายในไทย',      'admin', true),

    ('field.krw_per_thb.label',        'ko', '환율 (1 THB = ? KRW)',  'admin', true),
    ('field.krw_per_thb.label',        'en', 'FX rate (1 THB = ? KRW)','admin', true),
    ('field.krw_per_thb.label',        'th', 'อัตราแลกเปลี่ยน (1 THB = ? KRW)','admin', true),

    ('order.detail.financials.title',  'ko', '수익 분석',            'admin', true),
    ('order.detail.financials.title',  'en', 'Profit analysis',      'admin', true),
    ('order.detail.financials.title',  'th', 'การวิเคราะห์กำไร',       'admin', true),

    ('order.detail.financials.edit',   'ko', '재무 정보 수정',         'admin', true),
    ('order.detail.financials.edit',   'en', 'Edit financials',       'admin', true),
    ('order.detail.financials.edit',   'th', 'แก้ไขข้อมูลทางการเงิน',   'admin', true),

    ('order.detail.profit.label',      'ko', '순수익 (KRW)',          'admin', true),
    ('order.detail.profit.label',      'en', 'Net profit (KRW)',     'admin', true),
    ('order.detail.profit.label',      'th', 'กำไรสุทธิ (KRW)',         'admin', true),

    ('order.detail.profit.missing_fx', 'ko', '환율 미입력 — 계산 불가', 'admin', true),
    ('order.detail.profit.missing_fx', 'en', 'FX rate missing — cannot compute', 'admin', true),
    ('order.detail.profit.missing_fx', 'th', 'ไม่มีอัตราแลกเปลี่ยน — คำนวณไม่ได้', 'admin', true),

    ('order.detail.profit.incomplete', 'ko', '입력 미완료',            'admin', true),
    ('order.detail.profit.incomplete', 'en', 'Incomplete inputs',     'admin', true),
    ('order.detail.profit.incomplete', 'th', 'ข้อมูลไม่ครบ',            'admin', true),

    ('order.detail.paid_amount.edit',  'ko', '결제 금액 수정',         'admin', true),
    ('order.detail.paid_amount.edit',  'en', 'Edit paid amount',     'admin', true),
    ('order.detail.paid_amount.edit',  'th', 'แก้ไขยอดชำระ',           'admin', true),

    ('msg.deposit.paid_amount_correction', 'ko', '결제 금액 보정',     'admin', true),
    ('msg.deposit.paid_amount_correction', 'en', 'Paid amount correction', 'admin', true),
    ('msg.deposit.paid_amount_correction', 'th', 'การแก้ไขยอดชำระ',    'admin', true),

    ('dashboard.net_profit.title',     'ko', '총 순수익',              'admin', true),
    ('dashboard.net_profit.title',     'en', 'Total net profit',      'admin', true),
    ('dashboard.net_profit.title',     'th', 'กำไรสุทธิรวม',            'admin', true),

    ('dashboard.net_profit.help',      'ko', '재무 입력이 완료된 주문 합계만 포함',
                                       'admin', true),
    ('dashboard.net_profit.help',      'en', 'Includes only orders with complete financials',
                                       'admin', true),
    ('dashboard.net_profit.help',      'th', 'เฉพาะคำสั่งซื้อที่กรอกข้อมูลการเงินครบเท่านั้น',
                                       'admin', true);
