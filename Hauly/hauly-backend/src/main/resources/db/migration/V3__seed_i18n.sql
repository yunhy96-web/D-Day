-- =============================================================
-- V3__seed_i18n.sql  — UI 다국어 메시지 시드 (ko / en / th)
-- is_system = true : 코어 UI 키, 삭제 불가
-- context: 'common' | 'admin' | 'shop' | 'category.lens' | 'category.cosmetics'
-- =============================================================

-- -------------------------
-- 버튼 (btn.*)  — context: common
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('btn.cancel',              'ko', '취소',           'common', true),
    ('btn.cancel',              'en', 'Cancel',         'common', true),
    ('btn.cancel',              'th', 'ยกเลิก',         'common', true),
    ('btn.save',                'ko', '저장',            'common', true),
    ('btn.save',                'en', 'Save',           'common', true),
    ('btn.save',                'th', 'บันทึก',          'common', true),
    ('btn.submit',              'ko', '제출',            'common', true),
    ('btn.submit',              'en', 'Submit',         'common', true),
    ('btn.submit',              'th', 'ส่ง',             'common', true),
    ('btn.edit',                'ko', '수정',            'common', true),
    ('btn.edit',                'en', 'Edit',           'common', true),
    ('btn.edit',                'th', 'แก้ไข',           'common', true),
    ('btn.delete',              'ko', '삭제',            'common', true),
    ('btn.delete',              'en', 'Delete',         'common', true),
    ('btn.delete',              'th', 'ลบ',              'common', true),
    ('btn.register',            'ko', '등록',            'common', true),
    ('btn.register',            'en', 'Register',       'common', true),
    ('btn.register',            'th', 'ลงทะเบียน',       'common', true),
    ('btn.login',               'ko', '로그인',          'common', true),
    ('btn.login',               'en', 'Login',          'common', true),
    ('btn.login',               'th', 'เข้าสู่ระบบ',      'common', true),
    ('btn.logout',              'ko', '로그아웃',        'common', true),
    ('btn.logout',              'en', 'Logout',         'common', true),
    ('btn.logout',              'th', 'ออกจากระบบ',     'common', true),
    ('btn.search',              'ko', '검색',            'common', true),
    ('btn.search',              'en', 'Search',         'common', true),
    ('btn.search',              'th', 'ค้นหา',           'common', true),
    ('btn.confirm',             'ko', '확인',            'common', true),
    ('btn.confirm',             'en', 'Confirm',        'common', true),
    ('btn.confirm',             'th', 'ยืนยัน',          'common', true),
    ('btn.reject',              'ko', '거절',            'common', true),
    ('btn.reject',              'en', 'Reject',         'common', true),
    ('btn.reject',              'th', 'ปฏิเสธ',          'common', true),
    -- 주문 상태 전이 액션 버튼
    ('btn.ack',                 'ko', '의뢰 확인',       'admin', true),
    ('btn.ack',                 'en', 'Acknowledge',    'admin', true),
    ('btn.ack',                 'th', 'รับทราบ',         'admin', true),
    ('btn.start_buying',        'ko', '구매 시작',       'admin', true),
    ('btn.start_buying',        'en', 'Start Buying',   'admin', true),
    ('btn.start_buying',        'th', 'เริ่มซื้อ',        'admin', true),
    ('btn.mark_purchased',      'ko', '구매 완료 처리',  'admin', true),
    ('btn.mark_purchased',      'en', 'Mark Purchased', 'admin', true),
    ('btn.mark_purchased',      'th', 'ซื้อแล้ว',        'admin', true),
    ('btn.mark_shipped',        'ko', '발송 처리',       'admin', true),
    ('btn.mark_shipped',        'en', 'Mark Shipped',   'admin', true),
    ('btn.mark_shipped',        'th', 'จัดส่งแล้ว',      'admin', true),
    ('btn.mark_out_of_stock',   'ko', '품절 처리',       'admin', true),
    ('btn.mark_out_of_stock',   'en', 'Mark Out of Stock', 'admin', true),
    ('btn.mark_out_of_stock',   'th', 'สินค้าหมด',      'admin', true),
    ('btn.complete',            'ko', '완료 처리',       'admin', true),
    ('btn.complete',            'en', 'Complete',       'admin', true),
    ('btn.complete',            'th', 'เสร็จสิ้น',       'admin', true),
    ('btn.confirm_payment',     'ko', '결제 확인',       'admin', true),
    ('btn.confirm_payment',     'en', 'Confirm Payment','admin', true),
    ('btn.confirm_payment',     'th', 'ยืนยันชำระ',      'admin', true),
    ('btn.reject_payment',      'ko', '결제 거절',       'admin', true),
    ('btn.reject_payment',      'en', 'Reject Payment', 'admin', true),
    ('btn.reject_payment',      'th', 'ปฏิเสธชำระ',     'admin', true)
;

-- -------------------------
-- 메뉴 (menu.*)  — context: admin
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('menu.orders',     'ko', '주문 관리',   'admin', true),
    ('menu.orders',     'en', 'Orders',     'admin', true),
    ('menu.orders',     'th', 'ออเดอร์',    'admin', true),
    ('menu.customers',  'ko', '고객 관리',   'admin', true),
    ('menu.customers',  'en', 'Customers',  'admin', true),
    ('menu.customers',  'th', 'ลูกค้า',      'admin', true),
    ('menu.codes',      'ko', '공통코드',    'admin', true),
    ('menu.codes',      'en', 'Codes',      'admin', true),
    ('menu.codes',      'th', 'รหัส',        'admin', true),
    ('menu.i18n',       'ko', '다국어 관리', 'admin', true),
    ('menu.i18n',       'en', 'Messages',   'admin', true),
    ('menu.i18n',       'th', 'ข้อความ',    'admin', true),
    ('menu.categories', 'ko', '카테고리',    'admin', true),
    ('menu.categories', 'en', 'Categories', 'admin', true),
    ('menu.categories', 'th', 'หมวดหมู่',   'admin', true),
    ('menu.users',      'ko', '사용자 관리', 'admin', true),
    ('menu.users',      'en', 'Users',      'admin', true),
    ('menu.users',      'th', 'ผู้ใช้',      'admin', true)
;

-- -------------------------
-- 이행 상태 라벨 (status.fulfillment.*) — context: common
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('status.fulfillment.draft',            'ko', '작성 중',       'common', true),
    ('status.fulfillment.draft',            'en', 'Draft',         'common', true),
    ('status.fulfillment.draft',            'th', 'ร่าง',           'common', true),
    ('status.fulfillment.requested',        'ko', '의뢰 확정',      'common', true),
    ('status.fulfillment.requested',        'en', 'Requested',     'common', true),
    ('status.fulfillment.requested',        'th', 'ส่งคำสั่ง',      'common', true),
    ('status.fulfillment.acknowledged',     'ko', '확인 완료',      'common', true),
    ('status.fulfillment.acknowledged',     'en', 'Acknowledged',  'common', true),
    ('status.fulfillment.acknowledged',     'th', 'รับทราบแล้ว',   'common', true),
    ('status.fulfillment.purchasing',       'ko', '구매 진행 중',   'common', true),
    ('status.fulfillment.purchasing',       'en', 'Purchasing',    'common', true),
    ('status.fulfillment.purchasing',       'th', 'กำลังซื้อ',      'common', true),
    ('status.fulfillment.purchased',        'ko', '구매 완료',      'common', true),
    ('status.fulfillment.purchased',        'en', 'Purchased',     'common', true),
    ('status.fulfillment.purchased',        'th', 'ซื้อแล้ว',       'common', true),
    ('status.fulfillment.shipped_to_agent', 'ko', '배대지 발송',    'common', true),
    ('status.fulfillment.shipped_to_agent', 'en', 'Shipped',       'common', true),
    ('status.fulfillment.shipped_to_agent', 'th', 'จัดส่งแล้ว',    'common', true),
    ('status.fulfillment.completed',        'ko', '완료',           'common', true),
    ('status.fulfillment.completed',        'en', 'Completed',     'common', true),
    ('status.fulfillment.completed',        'th', 'เสร็จสิ้น',      'common', true),
    ('status.fulfillment.out_of_stock',     'ko', '품절',           'common', true),
    ('status.fulfillment.out_of_stock',     'en', 'Out of Stock',  'common', true),
    ('status.fulfillment.out_of_stock',     'th', 'สินค้าหมด',     'common', true),
    ('status.fulfillment.cancelled',        'ko', '취소',           'common', true),
    ('status.fulfillment.cancelled',        'en', 'Cancelled',     'common', true),
    ('status.fulfillment.cancelled',        'th', 'ยกเลิก',         'common', true),
    ('status.fulfillment.rejected',         'ko', '거절',           'common', true),
    ('status.fulfillment.rejected',         'en', 'Rejected',      'common', true),
    ('status.fulfillment.rejected',         'th', 'ถูกปฏิเสธ',     'common', true)
;

-- -------------------------
-- 결제 상태 라벨 (status.payment.*) — context: common
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('status.payment.not_required', 'ko', '해당없음',        'common', true),
    ('status.payment.not_required', 'en', 'Not Required',   'common', true),
    ('status.payment.not_required', 'th', 'ไม่ต้องชำระ',    'common', true),
    ('status.payment.pending',      'ko', '결제 대기',       'common', true),
    ('status.payment.pending',      'en', 'Pending',        'common', true),
    ('status.payment.pending',      'th', 'รอชำระ',          'common', true),
    ('status.payment.submitted',    'ko', '영수증 제출됨',   'common', true),
    ('status.payment.submitted',    'en', 'Submitted',      'common', true),
    ('status.payment.submitted',    'th', 'ส่งหลักฐาน',      'common', true),
    ('status.payment.confirmed',    'ko', '입금 확인됨',     'common', true),
    ('status.payment.confirmed',    'en', 'Confirmed',      'common', true),
    ('status.payment.confirmed',    'th', 'ยืนยันแล้ว',     'common', true),
    ('status.payment.rejected',     'ko', '영수증 거절',     'common', true),
    ('status.payment.rejected',     'en', 'Rejected',       'common', true),
    ('status.payment.rejected',     'th', 'ถูกปฏิเสธ',      'common', true)
;

-- -------------------------
-- 카테고리 렌즈 필드 라벨 (category.lens.*) — context: category.lens
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('category.lens.brand',       'ko', '브랜드',     'category.lens', true),
    ('category.lens.brand',       'en', 'Brand',      'category.lens', true),
    ('category.lens.brand',       'th', 'แบรนด์',     'category.lens', true),
    ('category.lens.model',       'ko', '시리즈/모델', 'category.lens', true),
    ('category.lens.model',       'en', 'Model',      'category.lens', true),
    ('category.lens.model',       'th', 'รุ่น',        'category.lens', true),
    ('category.lens.color',       'ko', '색상',        'category.lens', true),
    ('category.lens.color',       'en', 'Color',      'category.lens', true),
    ('category.lens.color',       'th', 'สี',           'category.lens', true),
    ('category.lens.wear_cycle',  'ko', '착용 주기',   'category.lens', true),
    ('category.lens.wear_cycle',  'en', 'Wear Cycle', 'category.lens', true),
    ('category.lens.wear_cycle',  'th', 'รอบการใส่',   'category.lens', true),
    ('category.lens.left_eye',    'ko', '왼쪽 눈',     'category.lens', true),
    ('category.lens.left_eye',    'en', 'Left Eye',   'category.lens', true),
    ('category.lens.left_eye',    'th', 'ตาซ้าย',      'category.lens', true),
    ('category.lens.right_eye',   'ko', '오른쪽 눈',   'category.lens', true),
    ('category.lens.right_eye',   'en', 'Right Eye',  'category.lens', true),
    ('category.lens.right_eye',   'th', 'ตาขวา',       'category.lens', true),
    ('category.lens.power',       'ko', '도수 (PWR)',  'category.lens', true),
    ('category.lens.power',       'en', 'Power (PWR)','category.lens', true),
    ('category.lens.power',       'th', 'ค่าสายตา',    'category.lens', true),
    ('category.lens.bc',          'ko', '베이스 커브 (BC)', 'category.lens', true),
    ('category.lens.bc',          'en', 'Base Curve (BC)', 'category.lens', true),
    ('category.lens.bc',          'th', 'ความโค้ง (BC)',   'category.lens', true),
    ('category.lens.dia',         'ko', '직경 (DIA)',  'category.lens', true),
    ('category.lens.dia',         'en', 'Diameter (DIA)', 'category.lens', true),
    ('category.lens.dia',         'th', 'เส้นผ่าน (DIA)',  'category.lens', true)
;

-- -------------------------
-- 공통 에러·안내 메시지 (msg.*) — context: common / admin
-- -------------------------
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('msg.error.required',           'ko', '필수 입력 항목입니다.',            'common', true),
    ('msg.error.required',           'en', 'This field is required.',        'common', true),
    ('msg.error.required',           'th', 'กรุณากรอกข้อมูล',               'common', true),
    ('msg.error.invalid',            'ko', '올바른 값을 입력해 주세요.',        'common', true),
    ('msg.error.invalid',            'en', 'Please enter a valid value.',    'common', true),
    ('msg.error.invalid',            'th', 'กรุณากรอกข้อมูลให้ถูกต้อง',    'common', true),
    ('msg.info.saved',               'ko', '저장되었습니다.',                  'common', true),
    ('msg.info.saved',               'en', 'Saved successfully.',            'common', true),
    ('msg.info.saved',               'th', 'บันทึกเรียบร้อยแล้ว',            'common', true),
    ('msg.info.deleted',             'ko', '삭제되었습니다.',                  'common', true),
    ('msg.info.deleted',             'en', 'Deleted successfully.',          'common', true),
    ('msg.info.deleted',             'th', 'ลบเรียบร้อยแล้ว',               'common', true),
    ('msg.order.out_of_stock_notify','ko', '품절이 발생했습니다. 처리 방법을 결정해 주세요.', 'admin', true),
    ('msg.order.out_of_stock_notify','en', 'Item is out of stock. Please decide how to proceed.', 'admin', true),
    ('msg.order.out_of_stock_notify','th', 'สินค้าหมด กรุณาตัดสินใจดำเนินการต่อ',  'admin', true),
    ('msg.order.cancel_reason_required', 'ko', '취소 사유를 선택해 주세요.',   'admin', true),
    ('msg.order.cancel_reason_required', 'en', 'Please select a cancel reason.', 'admin', true),
    ('msg.order.cancel_reason_required', 'th', 'กรุณาเลือกเหตุผลการยกเลิก',  'admin', true),
    ('msg.auth.login_required',      'ko', '로그인이 필요합니다.',             'common', true),
    ('msg.auth.login_required',      'en', 'Please log in to continue.',     'common', true),
    ('msg.auth.login_required',      'th', 'กรุณาเข้าสู่ระบบก่อน',           'common', true),
    ('msg.auth.unauthorized',        'ko', '접근 권한이 없습니다.',            'common', true),
    ('msg.auth.unauthorized',        'en', 'You do not have permission.',    'common', true),
    ('msg.auth.unauthorized',        'th', 'คุณไม่มีสิทธิ์เข้าถึง',          'common', true)
;
