-- =============================================================
-- V9__seed_url_search_sort_labels.sql
-- 1. product_url 라벨을 "구매 링크" 로 명확화
-- 2. 검색/정렬 UI 라벨 추가
-- =============================================================

UPDATE i18n_message SET message = '구매 링크'    WHERE message_key = 'field.product_url.label' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'Purchase Link' WHERE message_key = 'field.product_url.label' AND lang_code = 'en';
UPDATE i18n_message SET message = 'ลิงก์ซื้อ'      WHERE message_key = 'field.product_url.label' AND lang_code = 'th';

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.list.search.placeholder',   'ko', '주문번호 / 고객 / 상품명 검색',         'admin', true),
    ('order.list.search.placeholder',   'en', 'Search order no, customer or product','admin', true),
    ('order.list.search.placeholder',   'th', 'ค้นหาเลขที่ ลูกค้า สินค้า',              'admin', true),
    ('order.list.sort.label',           'ko', '정렬',                                   'admin', true),
    ('order.list.sort.label',           'en', 'Sort',                                  'admin', true),
    ('order.list.sort.label',           'th', 'เรียง',                                  'admin', true),
    ('order.list.sort.created_desc',    'ko', '최신순',                                 'admin', true),
    ('order.list.sort.created_desc',    'en', 'Newest first',                          'admin', true),
    ('order.list.sort.created_desc',    'th', 'ใหม่สุดก่อน',                            'admin', true),
    ('order.list.sort.created_asc',     'ko', '오래된순',                               'admin', true),
    ('order.list.sort.created_asc',     'en', 'Oldest first',                          'admin', true),
    ('order.list.sort.created_asc',     'th', 'เก่าสุดก่อน',                            'admin', true),
    ('order.list.sort.fulfillment',     'ko', '처리상태순',                             'admin', true),
    ('order.list.sort.fulfillment',     'en', 'By fulfillment status',                 'admin', true),
    ('order.list.sort.fulfillment',     'th', 'ตามสถานะ',                              'admin', true),
    ('order.list.sort.product_name',    'ko', '상품명순',                               'admin', true),
    ('order.list.sort.product_name',    'en', 'By product name',                       'admin', true),
    ('order.list.sort.product_name',    'th', 'ตามชื่อสินค้า',                          'admin', true);
