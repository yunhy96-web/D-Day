-- =============================================================
-- V14__seed_order_type_labels.sql
-- 세트 주문 기능에서 사용되는 UI 문자열을 ko/en/th로 i18n 화.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- 카드 제목 / 라디오 라벨
    ('order.create.order_type',                'ko', '주문 타입',                                    'admin', true),
    ('order.create.order_type',                'en', 'Order Type',                                    'admin', true),
    ('order.create.order_type',                'th', 'ประเภทคำสั่งซื้อ',                              'admin', true),

    ('order.type.individual',                  'ko', '개별 주문',                                    'admin', true),
    ('order.type.individual',                  'en', 'Individual',                                    'admin', true),
    ('order.type.individual',                  'th', 'รายการเดี่ยว',                                  'admin', true),

    ('order.type.set',                         'ko', '세트 주문 (2+1 등 묶음)',                       'admin', true),
    ('order.type.set',                         'en', 'Set Order (e.g. 2+1 bundle)',                   'admin', true),
    ('order.type.set',                         'th', 'คำสั่งซื้อแบบเซ็ต (เช่น 2+1)',                  'admin', true),

    ('order.type.set_help',                    'ko', '세트 구성품은 같은 카테고리여야 하며, 함께 주문/배송됩니다.', 'admin', true),
    ('order.type.set_help',                    'en', 'All items in a set must share the same category and ship together.', 'admin', true),
    ('order.type.set_help',                    'th', 'สินค้าทุกชิ้นในเซ็ตต้องอยู่ในหมวดหมู่เดียวกันและจัดส่งพร้อมกัน', 'admin', true),

    -- 아이템 카드 제목 (세트일 때)
    ('order.set_items.title',                  'ko', '세트 구성품',                                  'admin', true),
    ('order.set_items.title',                  'en', 'Set Items',                                     'admin', true),
    ('order.set_items.title',                  'th', 'สินค้าในเซ็ต',                                  'admin', true),

    -- 뱃지 (목록 / 상세)
    ('badge.set',                              'ko', '세트',                                          'admin', true),
    ('badge.set',                              'en', 'SET',                                           'admin', true),
    ('badge.set',                              'th', 'เซ็ต',                                          'admin', true),

    ('badge.set_order',                        'ko', '세트 주문',                                    'admin', true),
    ('badge.set_order',                        'en', 'Set Order',                                     'admin', true),
    ('badge.set_order',                        'th', 'คำสั่งซื้อแบบเซ็ต',                              'admin', true),

    -- 클라이언트 검증 에러
    ('msg.error.set_category_mismatch',        'ko', '세트 안의 모든 항목은 같은 카테고리여야 합니다.', 'admin', true),
    ('msg.error.set_category_mismatch',        'en', 'All items in a set must use the same category.', 'admin', true),
    ('msg.error.set_category_mismatch',        'th', 'สินค้าทุกชิ้นในเซ็ตต้องใช้หมวดหมู่เดียวกัน',  'admin', true);
