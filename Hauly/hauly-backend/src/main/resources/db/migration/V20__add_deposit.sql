-- =============================================================
-- V20__add_deposit.sql
-- 디파짓(예치금) 잔액 관리 — 여친이 나에게 준 KRW 잔액에서 차감하며
-- 주문 PURCHASED 시 자동 차감, CANCELLED 시 자동 환원.
--
-- 단일 글로벌 잔액 (현재 selim ↔ union 1관계). 잔액은 별도 캐시 테이블 없이
-- SUM(amount_krw) 집계로 계산. 트랜잭션은 append-only.
-- 잔액 음수 허용 — 마이너스면 여친이 나에게 줘야 하는 돈.
-- =============================================================

CREATE TABLE deposit_transaction (
    id                BIGSERIAL PRIMARY KEY,
    kind              VARCHAR(16) NOT NULL,        -- TOP_UP / PURCHASE / REFUND / ADJUSTMENT
    amount_krw        NUMERIC(15, 2) NOT NULL,     -- signed; positive = credit, negative = debit
    related_order_id  BIGINT REFERENCES "order"(id) ON DELETE SET NULL,
    note              TEXT,
    created_by        BIGINT REFERENCES app_user(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Ledger ordering — most-recent first.
CREATE INDEX idx_deposit_tx_created ON deposit_transaction (created_at DESC);

-- Refund lookups — partial index since most rows have NULL related_order_id.
CREATE INDEX idx_deposit_tx_order ON deposit_transaction (related_order_id)
    WHERE related_order_id IS NOT NULL;

-- ----- i18n 라벨 (ko/en/th) -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- 메뉴
    ('menu.deposits',                  'ko', '디파짓',                       'admin', true),
    ('menu.deposits',                  'en', 'Deposit',                      'admin', true),
    ('menu.deposits',                  'th', 'เงินฝาก',                       'admin', true),

    -- 페이지/카드 타이틀
    ('deposit.title',                  'ko', '디파짓 잔액',                   'admin', true),
    ('deposit.title',                  'en', 'Deposit balance',              'admin', true),
    ('deposit.title',                  'th', 'ยอดเงินฝาก',                    'admin', true),

    ('deposit.balance.label',          'ko', '현재 잔액',                    'admin', true),
    ('deposit.balance.label',          'en', 'Current balance',              'admin', true),
    ('deposit.balance.label',          'th', 'ยอดคงเหลือปัจจุบัน',              'admin', true),

    ('deposit.negative_hint',          'ko', '잔액이 마이너스이면 여친이 나에게 줘야 하는 돈입니다.',
                                          'admin', true),
    ('deposit.negative_hint',          'en', 'A negative balance means money owed to you.',
                                          'admin', true),
    ('deposit.negative_hint',          'th', 'ยอดติดลบหมายถึงเงินที่ค้างชำระคุณ',
                                          'admin', true),

    -- 트랜잭션 종류
    ('deposit.tx.kind.top_up',         'ko', '입금',                         'admin', true),
    ('deposit.tx.kind.top_up',         'en', 'Top-up',                       'admin', true),
    ('deposit.tx.kind.top_up',         'th', 'เติมเงิน',                       'admin', true),

    ('deposit.tx.kind.purchase',       'ko', '구매 차감',                    'admin', true),
    ('deposit.tx.kind.purchase',       'en', 'Purchase',                     'admin', true),
    ('deposit.tx.kind.purchase',       'th', 'ค่าซื้อสินค้า',                    'admin', true),

    ('deposit.tx.kind.refund',         'ko', '취소 환원',                    'admin', true),
    ('deposit.tx.kind.refund',         'en', 'Refund',                       'admin', true),
    ('deposit.tx.kind.refund',         'th', 'คืนเงิน',                        'admin', true),

    ('deposit.tx.kind.adjustment',     'ko', '수동 보정',                    'admin', true),
    ('deposit.tx.kind.adjustment',     'en', 'Adjustment',                   'admin', true),
    ('deposit.tx.kind.adjustment',     'th', 'ปรับปรุงด้วยตนเอง',                'admin', true),

    -- 트랜잭션 테이블 헤더
    ('deposit.tx.col.created_at',      'ko', '시각',                         'admin', true),
    ('deposit.tx.col.created_at',      'en', 'Time',                         'admin', true),
    ('deposit.tx.col.created_at',      'th', 'เวลา',                          'admin', true),

    ('deposit.tx.col.kind',            'ko', '종류',                         'admin', true),
    ('deposit.tx.col.kind',            'en', 'Kind',                         'admin', true),
    ('deposit.tx.col.kind',            'th', 'ประเภท',                        'admin', true),

    ('deposit.tx.col.amount',          'ko', '금액',                         'admin', true),
    ('deposit.tx.col.amount',          'en', 'Amount',                       'admin', true),
    ('deposit.tx.col.amount',          'th', 'จำนวนเงิน',                      'admin', true),

    ('deposit.tx.col.order',           'ko', '주문',                         'admin', true),
    ('deposit.tx.col.order',           'en', 'Order',                        'admin', true),
    ('deposit.tx.col.order',           'th', 'คำสั่งซื้อ',                       'admin', true),

    ('deposit.tx.col.note',            'ko', '메모',                         'admin', true),
    ('deposit.tx.col.note',            'en', 'Note',                         'admin', true),
    ('deposit.tx.col.note',            'th', 'หมายเหตุ',                       'admin', true),

    ('deposit.tx.col.actor',           'ko', '처리자',                       'admin', true),
    ('deposit.tx.col.actor',           'en', 'Actor',                        'admin', true),
    ('deposit.tx.col.actor',           'th', 'ผู้ดำเนินการ',                     'admin', true),

    ('deposit.tx.empty',               'ko', '거래 내역이 없습니다.',          'admin', true),
    ('deposit.tx.empty',               'en', 'No transactions yet.',         'admin', true),
    ('deposit.tx.empty',               'th', 'ยังไม่มีรายการ',                 'admin', true),

    -- 수동 보정 폼
    ('deposit.adjust.button',          'ko', '잔액 보정',                    'admin', true),
    ('deposit.adjust.button',          'en', 'Adjust balance',               'admin', true),
    ('deposit.adjust.button',          'th', 'ปรับยอด',                       'admin', true),

    ('deposit.adjust.title',           'ko', '디파짓 잔액 수동 보정',         'admin', true),
    ('deposit.adjust.title',           'en', 'Manual deposit adjustment',    'admin', true),
    ('deposit.adjust.title',           'th', 'ปรับยอดเงินฝากด้วยตนเอง',          'admin', true),

    ('deposit.adjust.amount',          'ko', '금액 (KRW)',                   'admin', true),
    ('deposit.adjust.amount',          'en', 'Amount (KRW)',                 'admin', true),
    ('deposit.adjust.amount',          'th', 'จำนวน (KRW)',                   'admin', true),

    ('deposit.adjust.amount.help',     'ko', '양수 = 입금, 음수 = 차감. 0은 불가.',
                                          'admin', true),
    ('deposit.adjust.amount.help',     'en', 'Positive = credit, negative = debit. Zero is not allowed.',
                                          'admin', true),
    ('deposit.adjust.amount.help',     'th', 'บวก = ฝาก, ลบ = หัก ห้ามใส่ 0',
                                          'admin', true),

    ('deposit.adjust.note',            'ko', '메모 (필수)',                  'admin', true),
    ('deposit.adjust.note',            'en', 'Note (required)',              'admin', true),
    ('deposit.adjust.note',            'th', 'หมายเหตุ (จำเป็น)',                'admin', true),

    ('deposit.adjust.confirm',         'ko', '보정 적용',                    'admin', true),
    ('deposit.adjust.confirm',         'en', 'Apply adjustment',             'admin', true),
    ('deposit.adjust.confirm',         'th', 'ยืนยันการปรับ',                   'admin', true),

    -- PURCHASED 전환 모달
    ('order.purchased.modal.title',    'ko', '구매 완료 처리',                'admin', true),
    ('order.purchased.modal.title',    'en', 'Mark as purchased',            'admin', true),
    ('order.purchased.modal.title',    'th', 'ทำเครื่องหมายว่าซื้อแล้ว',           'admin', true),

    ('order.purchased.modal.help',     'ko', '실제 결제한 금액을 입력하면 디파짓에서 자동 차감됩니다.',
                                          'admin', true),
    ('order.purchased.modal.help',     'en', 'The amount actually paid will be debited from the deposit.',
                                          'admin', true),
    ('order.purchased.modal.help',     'th', 'จำนวนเงินที่จ่ายจริงจะถูกหักจากเงินฝากโดยอัตโนมัติ',
                                          'admin', true),

    ('order.purchased.modal.paid',     'ko', '실결제 금액 (KRW)',             'admin', true),
    ('order.purchased.modal.paid',     'en', 'Paid amount (KRW)',            'admin', true),
    ('order.purchased.modal.paid',     'th', 'จำนวนเงินที่จ่ายจริง (KRW)',         'admin', true),

    ('order.purchased.modal.confirm',  'ko', '구매 완료',                    'admin', true),
    ('order.purchased.modal.confirm',  'en', 'Mark purchased',               'admin', true),
    ('order.purchased.modal.confirm',  'th', 'ยืนยันการซื้อ',                   'admin', true),

    -- 에러
    ('msg.error.deposit_amount_invalid', 'ko', '금액은 0이 아닌 값이어야 합니다.',
                                          'admin', true),
    ('msg.error.deposit_amount_invalid', 'en', 'Amount must be non-zero.',
                                          'admin', true),
    ('msg.error.deposit_amount_invalid', 'th', 'จำนวนต้องไม่เป็นศูนย์',
                                          'admin', true),

    ('msg.error.paid_amount_required', 'ko', 'PURCHASED 전환에는 실결제 금액이 필요합니다.',
                                          'admin', true),
    ('msg.error.paid_amount_required', 'en', 'Paid amount is required when transitioning to PURCHASED.',
                                          'admin', true),
    ('msg.error.paid_amount_required', 'th', 'ต้องระบุจำนวนเงินที่จ่ายจริงเมื่อเปลี่ยนเป็น PURCHASED',
                                          'admin', true);
