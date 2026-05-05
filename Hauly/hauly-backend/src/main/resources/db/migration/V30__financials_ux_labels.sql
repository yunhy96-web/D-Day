-- =============================================================
-- V30__financials_ux_labels.sql
-- 수익 분석 카드 UX 개선용 i18n 추가:
--   - amount_currency_mismatch: 금액만 또는 통화만 입력했을 때 모달 inline 에러
--   - fx_required_hint: THB 통화 사용 시 환율 입력 필요 안내
--   - paid_amount.input: 결제금액이 null 일 때 "입력" 버튼 라벨
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('msg.error.amount_currency_mismatch', 'ko', '금액과 통화를 함께 입력하세요',                'admin', true),
    ('msg.error.amount_currency_mismatch', 'en', 'Enter amount and currency together',          'admin', true),
    ('msg.error.amount_currency_mismatch', 'th', 'กรุณากรอกจำนวนและสกุลเงินพร้อมกัน',             'admin', true),

    ('order.detail.financials.fx_required_hint', 'ko', 'THB 금액 입력 시 환율(1 THB = ? KRW) 필수',
                                                  'admin', true),
    ('order.detail.financials.fx_required_hint', 'en', 'FX rate required when any THB amount is entered',
                                                  'admin', true),
    ('order.detail.financials.fx_required_hint', 'th', 'ต้องใส่อัตราแลกเปลี่ยนเมื่อมีจำนวนเป็น THB',
                                                  'admin', true),

    ('order.detail.paid_amount.input',     'ko', '결제 금액 입력',         'admin', true),
    ('order.detail.paid_amount.input',     'en', 'Enter paid amount',     'admin', true),
    ('order.detail.paid_amount.input',     'th', 'กรอกยอดชำระ',           'admin', true);
