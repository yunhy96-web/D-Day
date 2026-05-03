-- =============================================================
-- V7__seed_i18n_for_intake_pages.sql
-- INTAKE 주문 등록/목록/상세 페이지에서 사용하는 UI 메시지 시드.
-- 기존 V3 패턴 따름 (ko / en / th, is_system = true).
-- =============================================================

-- ----- 메뉴 추가분 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('menu.dashboard',    'ko', '대시보드',           'admin', true),
    ('menu.dashboard',    'en', 'Dashboard',         'admin', true),
    ('menu.dashboard',    'th', 'แดชบอร์ด',           'admin', true),
    ('menu.orders_new',   'ko', '주문 등록',          'admin', true),
    ('menu.orders_new',   'en', 'New Order',         'admin', true),
    ('menu.orders_new',   'th', 'เพิ่มคำสั่งซื้อ',      'admin', true);

-- ----- menu.orders 기존 라벨이 "주문 관리" 였는데, MVP 에선 "주문 목록" 이 더 적합 -----
UPDATE i18n_message SET message = '주문 목록'         WHERE message_key = 'menu.orders' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'Orders'           WHERE message_key = 'menu.orders' AND lang_code = 'en';
UPDATE i18n_message SET message = 'รายการคำสั่งซื้อ' WHERE message_key = 'menu.orders' AND lang_code = 'th';

-- ----- 버튼 추가분 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('btn.saving',  'ko', '저장 중...',     'common', true),
    ('btn.saving',  'en', 'Saving...',     'common', true),
    ('btn.saving',  'th', 'กำลังบันทึก...', 'common', true),
    ('btn.reload',  'ko', '새로고침',       'common', true),
    ('btn.reload',  'en', 'Reload',        'common', true),
    ('btn.reload',  'th', 'โหลดใหม่',      'common', true);

-- ----- 메시지/에러 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('msg.loading',                       'ko', '불러오는 중...',                          'common', true),
    ('msg.loading',                       'en', 'Loading...',                              'common', true),
    ('msg.loading',                       'th', 'กำลังโหลด...',                            'common', true),
    ('msg.error.at_least_one_item',       'ko', '최소 1개 이상의 상품을 등록해주세요.',     'common', true),
    ('msg.error.at_least_one_item',       'en', 'Please add at least one item.',           'common', true),
    ('msg.error.at_least_one_item',       'th', 'กรุณาเพิ่มสินค้าอย่างน้อย 1 รายการ',       'common', true),
    ('msg.error.INVALID_STATUS_TRANSITION','ko', '현재 상태에서 변경할 수 없습니다.',        'common', true),
    ('msg.error.INVALID_STATUS_TRANSITION','en', 'Status change is not allowed from the current state.','common', true),
    ('msg.error.INVALID_STATUS_TRANSITION','th', 'ไม่สามารถเปลี่ยนสถานะจากสถานะปัจจุบันได้','common', true);

-- ----- 입력 필드 라벨 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.customer_name.label', 'ko', '고객명',          'admin', true),
    ('field.customer_name.label', 'en', 'Customer Name',   'admin', true),
    ('field.customer_name.label', 'th', 'ชื่อลูกค้า',        'admin', true),
    ('field.line_id.label',       'ko', 'LINE ID',         'admin', true),
    ('field.line_id.label',       'en', 'LINE ID',         'admin', true),
    ('field.line_id.label',       'th', 'LINE ID',         'admin', true),
    ('field.phone.label',         'ko', '전화번호',         'admin', true),
    ('field.phone.label',         'en', 'Phone',           'admin', true),
    ('field.phone.label',         'th', 'เบอร์โทร',         'admin', true),
    ('field.product_name.label',  'ko', '상품명',           'admin', true),
    ('field.product_name.label',  'en', 'Product Name',    'admin', true),
    ('field.product_name.label',  'th', 'ชื่อสินค้า',         'admin', true),
    ('field.product_url.label',   'ko', '상품 URL',         'admin', true),
    ('field.product_url.label',   'en', 'Product URL',     'admin', true),
    ('field.product_url.label',   'th', 'URL สินค้า',       'admin', true),
    ('field.quantity.label',      'ko', '수량',             'admin', true),
    ('field.quantity.label',      'en', 'Quantity',        'admin', true),
    ('field.quantity.label',      'th', 'จำนวน',           'admin', true),
    ('field.customer_memo.label', 'ko', '고객 메모',        'admin', true),
    ('field.customer_memo.label', 'en', 'Customer Memo',   'admin', true),
    ('field.customer_memo.label', 'th', 'บันทึกลูกค้า',      'admin', true),
    ('field.internal_memo.label', 'ko', '내부 메모',        'admin', true),
    ('field.internal_memo.label', 'en', 'Internal Memo',   'admin', true),
    ('field.internal_memo.label', 'th', 'บันทึกภายใน',     'admin', true);

-- ----- 주문 목록 페이지 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.list.title',  'ko', '주문 목록',              'admin', true),
    ('order.list.title',  'en', 'Orders',                'admin', true),
    ('order.list.title',  'th', 'รายการคำสั่งซื้อ',         'admin', true),
    ('order.list.empty',  'ko', '등록된 주문이 없습니다.', 'admin', true),
    ('order.list.empty',  'en', 'No orders yet.',         'admin', true),
    ('order.list.empty',  'th', 'ยังไม่มีคำสั่งซื้อ',          'admin', true);

-- ----- 주문 등록 페이지 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.create.title',          'ko', '주문 등록',                                                 'admin', true),
    ('order.create.title',          'en', 'New Order',                                                 'admin', true),
    ('order.create.title',          'th', 'เพิ่มคำสั่งซื้อ',                                              'admin', true),
    ('order.create.customer',       'ko', '고객 정보',                                                 'admin', true),
    ('order.create.customer',       'en', 'Customer',                                                  'admin', true),
    ('order.create.customer',       'th', 'ข้อมูลลูกค้า',                                                'admin', true),
    ('order.create.items',          'ko', '상품 항목',                                                 'admin', true),
    ('order.create.items',          'en', 'Items',                                                     'admin', true),
    ('order.create.items',          'th', 'สินค้า',                                                     'admin', true),
    ('order.create.memo',           'ko', '메모',                                                      'admin', true),
    ('order.create.memo',           'en', 'Memo',                                                      'admin', true),
    ('order.create.memo',           'th', 'บันทึก',                                                     'admin', true),
    ('order.create.add_item',       'ko', '상품 추가',                                                 'admin', true),
    ('order.create.add_item',       'en', 'Add Item',                                                  'admin', true),
    ('order.create.add_item',       'th', 'เพิ่มสินค้า',                                                 'admin', true),
    ('order.create.matching_hint',  'ko', 'LINE ID 또는 전화번호로 기존 고객이 자동 매칭됩니다.',         'admin', true),
    ('order.create.matching_hint',  'en', 'Existing customers are auto-matched by LINE ID or phone.',   'admin', true),
    ('order.create.matching_hint',  'th', 'ลูกค้าเดิมจะจับคู่อัตโนมัติด้วย LINE ID หรือเบอร์โทร',           'admin', true);

-- ----- 주문 상세 페이지 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.detail.customer',         'ko', '고객 정보',                  'admin', true),
    ('order.detail.customer',         'en', 'Customer',                   'admin', true),
    ('order.detail.customer',         'th', 'ลูกค้า',                      'admin', true),
    ('order.detail.items',            'ko', '주문 상품',                  'admin', true),
    ('order.detail.items',            'en', 'Items',                      'admin', true),
    ('order.detail.items',            'th', 'สินค้า',                      'admin', true),
    ('order.detail.memo',             'ko', '메모',                       'admin', true),
    ('order.detail.memo',             'en', 'Memo',                       'admin', true),
    ('order.detail.memo',             'th', 'บันทึก',                     'admin', true),
    ('order.detail.history',          'ko', '상태 이력',                  'admin', true),
    ('order.detail.history',          'en', 'History',                    'admin', true),
    ('order.detail.history',          'th', 'ประวัติ',                    'admin', true),
    ('order.detail.note_placeholder', 'ko', '변경 사유 (선택)',           'admin', true),
    ('order.detail.note_placeholder', 'en', 'Reason for change (optional)','admin', true),
    ('order.detail.note_placeholder', 'th', 'เหตุผลในการเปลี่ยน (ไม่บังคับ)','admin', true),
    ('order.detail.terminal',         'ko', '최종 상태입니다.',            'admin', true),
    ('order.detail.terminal',         'en', 'Terminal state.',            'admin', true),
    ('order.detail.terminal',         'th', 'สถานะสุดท้าย',                'admin', true);

-- ----- 주문 목록 컬럼 헤더 -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.col.no',           'ko', '주문번호',          'admin', true),
    ('order.col.no',           'en', 'No.',              'admin', true),
    ('order.col.no',           'th', 'เลขที่',            'admin', true),
    ('order.col.customer',     'ko', '고객',              'admin', true),
    ('order.col.customer',     'en', 'Customer',          'admin', true),
    ('order.col.customer',     'th', 'ลูกค้า',            'admin', true),
    ('order.col.items',        'ko', '상품 수',           'admin', true),
    ('order.col.items',        'en', 'Items',            'admin', true),
    ('order.col.items',        'th', 'จำนวนสินค้า',       'admin', true),
    ('order.col.fulfillment',  'ko', '처리 상태',         'admin', true),
    ('order.col.fulfillment',  'en', 'Fulfillment',      'admin', true),
    ('order.col.fulfillment',  'th', 'สถานะดำเนินการ',    'admin', true),
    ('order.col.payment',      'ko', '결제 상태',         'admin', true),
    ('order.col.payment',      'en', 'Payment',          'admin', true),
    ('order.col.payment',      'th', 'สถานะชำระเงิน',     'admin', true),
    ('order.col.created_at',   'ko', '등록일시',          'admin', true),
    ('order.col.created_at',   'en', 'Created',          'admin', true),
    ('order.col.created_at',   'th', 'วันที่สร้าง',       'admin', true);
