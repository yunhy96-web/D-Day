-- =============================================================
-- V2__seed_common_code.sql  — 시스템 공통코드 시드
-- is_system = true : 애플리케이션 로직이 의존하는 코드, 삭제 불가
-- =============================================================

-- -------------------------
-- Groups
-- -------------------------
INSERT INTO common_code_group (group_code, group_name_ko, group_name_en, description, is_system) VALUES
    ('ORDER_ORIGIN',          '주문 출처',        'Order Origin',            '주문이 Admin에서 생성됐는지 마켓에서 생성됐는지 구분', true),
    ('FULFILLMENT_STATUS',    '처리 상태',        'Fulfillment Status',       '주문 처리(구매·배송) 진행 상태',                   true),
    ('PAYMENT_STATUS',        '결제 상태',        'Payment Status',           '마켓 주문의 결제 흐름 상태',                      true),
    ('PAYMENT_METHOD',        '결제 수단',        'Payment Method',           '고객이 사용한 결제 수단',                         false),
    ('COURIER_KR',            '한국 택배사',      'Korean Courier',           '한국 내 택배/발송사',                             false),
    ('USER_ROLE',             '운영자 역할',      'User Role',               '백오피스 운영자 역할 분류',                         true),
    ('ACCOUNT_STATUS',        '계정 유형',        'Account Status',           '고객 계정 유형 (게스트 / 회원)',                   true),
    ('LANG',                  '지원 언어',        'Supported Language',       '시스템에서 지원하는 언어 코드',                     true),
    ('CANCEL_REASON',         '취소 사유',        'Cancel Reason',            '주문 취소 사유',                                false),
    ('REJECT_REASON',         '거절 사유',        'Reject Reason',            '주문 거절 사유',                                false),
    ('PRODUCT_STATUS',        '상품 상태',        'Product Status',           '마켓 상품 노출 상태',                             true),
    ('LENS_WEAR_CYCLE',       '렌즈 착용 주기',   'Lens Wear Cycle',          '콘택트렌즈 착용(교체) 주기',                       true),
    ('OUT_OF_STOCK_DECISION', '품절 처리 결정',   'Out-of-Stock Decision',    '품절 발견 시 INTAKE 결정 유형',                    true)
;

-- -------------------------
-- ORDER_ORIGIN
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('ORDER_ORIGIN', 'ADMIN_INTAKE', 'Admin 의뢰', 'Admin Intake', 'Admin Intake', 10, true),
    ('ORDER_ORIGIN', 'MARKETPLACE',  '마켓 주문',  'Marketplace',  'Marketplace',  20, true)
;

-- -------------------------
-- FULFILLMENT_STATUS
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('FULFILLMENT_STATUS', 'DRAFT',            '작성 중',        'Draft',              'ร่าง',                 10, true),
    ('FULFILLMENT_STATUS', 'REQUESTED',        '의뢰 확정',      'Requested',          'ส่งคำสั่ง',             20, true),
    ('FULFILLMENT_STATUS', 'ACKNOWLEDGED',     '확인 완료',      'Acknowledged',       'รับทราบแล้ว',           30, true),
    ('FULFILLMENT_STATUS', 'PURCHASING',       '구매 진행 중',   'Purchasing',         'กำลังซื้อ',             40, true),
    ('FULFILLMENT_STATUS', 'PURCHASED',        '구매 완료',      'Purchased',          'ซื้อแล้ว',              50, true),
    ('FULFILLMENT_STATUS', 'SHIPPED_TO_AGENT', '배대지 발송',    'Shipped to Agent',   'จัดส่งแล้ว',            60, true),
    ('FULFILLMENT_STATUS', 'COMPLETED',        '완료',           'Completed',          'เสร็จสิ้น',             70, true),
    ('FULFILLMENT_STATUS', 'OUT_OF_STOCK',     '품절',           'Out of Stock',       'สินค้าหมด',             80, true),
    ('FULFILLMENT_STATUS', 'CANCELLED',        '취소',           'Cancelled',          'ยกเลิก',                90, true),
    ('FULFILLMENT_STATUS', 'REJECTED',         '거절',           'Rejected',           'ถูกปฏิเสธ',             100, true)
;

-- -------------------------
-- PAYMENT_STATUS
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('PAYMENT_STATUS', 'NOT_REQUIRED', '해당없음',        'Not Required', 'ไม่ต้องชำระ',  10, true),
    ('PAYMENT_STATUS', 'PENDING',      '결제 대기',       'Pending',      'รอชำระ',       20, true),
    ('PAYMENT_STATUS', 'SUBMITTED',    '영수증 제출됨',   'Submitted',    'ส่งหลักฐาน',   30, true),
    ('PAYMENT_STATUS', 'CONFIRMED',    '입금 확인됨',     'Confirmed',    'ยืนยันแล้ว',   40, true),
    ('PAYMENT_STATUS', 'REJECTED',     '영수증 거절',     'Rejected',     'ถูกปฏิเสธ',    50, true)
;

-- -------------------------
-- PAYMENT_METHOD
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('PAYMENT_METHOD', 'THAI_QR',          'QR 결제 (태국)',  'Thai QR',            'QR Pay',       10, true),
    ('PAYMENT_METHOD', 'BANK_TRANSFER_TH', '태국 계좌이체', 'Bank Transfer (TH)',  'โอนธนาคาร (TH)', 20, true),
    ('PAYMENT_METHOD', 'BANK_TRANSFER_KR', '한국 계좌이체', 'Bank Transfer (KR)',  'โอนธนาคาร (KR)', 30, true),
    ('PAYMENT_METHOD', 'NONE',             '없음',           'None',                'ไม่มี',         99, true)
;

-- -------------------------
-- COURIER_KR
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('COURIER_KR', 'CJ',          'CJ대한통운', 'CJ Logistics', 'CJ',          10, true),
    ('COURIER_KR', 'HANJIN',      '한진택배',   'Hanjin',       'Hanjin',       20, true),
    ('COURIER_KR', 'KOREA_POST',  '우체국택배', 'Korea Post',   'Korea Post',   30, true),
    ('COURIER_KR', 'LOTTE',       '롯데택배',   'Lotte',        'Lotte',        40, true),
    ('COURIER_KR', 'ROZEN',       '로젠택배',   'Rozen',        'Rozen',        50, true),
    ('COURIER_KR', 'OTHER',       '기타',       'Other',        'อื่นๆ',        99, false)
;

-- -------------------------
-- USER_ROLE
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('USER_ROLE', 'INTAKE', '의뢰 접수', 'Intake',     'รับออเดอร์', 10, true),
    ('USER_ROLE', 'BUYER',  '구매 담당', 'Buyer',      'ผู้ซื้อ',    20, true),
    ('USER_ROLE', 'ADMIN',  '관리자',    'Admin',      'ผู้ดูแล',    30, true)
;

-- -------------------------
-- ACCOUNT_STATUS
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('ACCOUNT_STATUS', 'GUEST',      '게스트',    'Guest',      'ผู้เยี่ยมชม',  10, true),
    ('ACCOUNT_STATUS', 'REGISTERED', '회원',      'Registered', 'สมาชิก',       20, true)
;

-- -------------------------
-- LANG
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('LANG', 'KO', '한국어', 'Korean',  'เกาหลี',   10, true),
    ('LANG', 'EN', '영어',   'English', 'อังกฤษ',   20, true),
    ('LANG', 'TH', '태국어', 'Thai',    'ไทย',       30, true)
;

-- -------------------------
-- CANCEL_REASON
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('CANCEL_REASON', 'CUSTOMER_REQUEST', '고객 요청',      'Customer Request', 'ลูกค้าขอยกเลิก',  10, true),
    ('CANCEL_REASON', 'OUT_OF_STOCK',     '품절',           'Out of Stock',     'สินค้าหมด',         20, true),
    ('CANCEL_REASON', 'BUDGET_EXCEEDED',  '예산 초과',      'Budget Exceeded',  'งบเกิน',            30, true),
    ('CANCEL_REASON', 'OTHER',            '기타',           'Other',            'อื่นๆ',              99, false)
;

-- -------------------------
-- REJECT_REASON
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('REJECT_REASON', 'ITEM_UNAVAILABLE',     '구매 불가 상품',  'Item Unavailable',     'หาสินค้าไม่ได้',    10, true),
    ('REJECT_REASON', 'SHIPPING_RESTRICTED',  '배송 불가',       'Shipping Restricted',  'จัดส่งไม่ได้',      20, true),
    ('REJECT_REASON', 'PAYMENT_INVALID',      '결제 확인 불가',  'Payment Invalid',       'หลักฐานไม่ถูกต้อง', 30, true),
    ('REJECT_REASON', 'OTHER',                '기타',            'Other',                 'อื่นๆ',              99, false)
;

-- -------------------------
-- PRODUCT_STATUS
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('PRODUCT_STATUS', 'DRAFT',    '초안',   'Draft',    'ร่าง',        10, true),
    ('PRODUCT_STATUS', 'ACTIVE',   '판매 중', 'Active',  'ใช้งาน',      20, true),
    ('PRODUCT_STATUS', 'ARCHIVED', '보관',   'Archived', 'เก็บถาวร',    30, true)
;

-- -------------------------
-- LENS_WEAR_CYCLE
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('LENS_WEAR_CYCLE', '1DAY',   '1일 (일회용)', '1 Day',    '1 วัน',    10, true),
    ('LENS_WEAR_CYCLE', '2WEEK',  '2주',          '2 Week',   '2 สัปดาห์', 20, true),
    ('LENS_WEAR_CYCLE', '1MONTH', '1개월',        '1 Month',  '1 เดือน',  30, true),
    ('LENS_WEAR_CYCLE', '3MONTH', '3개월',        '3 Month',  '3 เดือน',  40, true),
    ('LENS_WEAR_CYCLE', '6MONTH', '6개월',        '6 Month',  '6 เดือน',  50, true),
    ('LENS_WEAR_CYCLE', '1YEAR',  '1년',          '1 Year',   '1 ปี',     60, true)
;

-- -------------------------
-- OUT_OF_STOCK_DECISION
-- -------------------------
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('OUT_OF_STOCK_DECISION', 'REPLACE', '대안 상품으로 교체', 'Replace',      'เปลี่ยนสินค้า', 10, true),
    ('OUT_OF_STOCK_DECISION', 'WAIT',    '재입고 대기',        'Wait',         'รอสินค้า',       20, true),
    ('OUT_OF_STOCK_DECISION', 'CANCEL',  '취소',              'Cancel Order', 'ยกเลิก',          30, true)
;
