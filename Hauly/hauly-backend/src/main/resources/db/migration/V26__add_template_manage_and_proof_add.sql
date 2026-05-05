-- =============================================================
-- V26__add_template_manage_and_proof_add.sql
-- 1) 배송지 템플릿 관리 모달용 i18n
-- 2) PURCHASED 이후에도 결제 증빙 사진 추가/삭제용 i18n
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('shipping.template.manage',         'ko', '관리',                    'admin', true),
    ('shipping.template.manage',         'en', 'Manage',                  'admin', true),
    ('shipping.template.manage',         'th', 'จัดการ',                   'admin', true),

    ('shipping.template.manage.title',   'ko', '저장된 배송지 관리',       'admin', true),
    ('shipping.template.manage.title',   'en', 'Manage saved addresses',  'admin', true),
    ('shipping.template.manage.title',   'th', 'จัดการที่อยู่ที่บันทึกไว้',  'admin', true),

    ('shipping.template.delete.confirm', 'ko', '이 배송지를 삭제하시겠습니까?',
                                        'admin', true),
    ('shipping.template.delete.confirm', 'en', 'Delete this address?',
                                        'admin', true),
    ('shipping.template.delete.confirm', 'th', 'ลบที่อยู่นี้หรือไม่?',
                                        'admin', true),

    ('order.proof.add.button',           'ko', '증빙 사진 추가',           'admin', true),
    ('order.proof.add.button',           'en', 'Add proof images',        'admin', true),
    ('order.proof.add.button',           'th', 'เพิ่มหลักฐานการชำระ',     'admin', true),

    ('order.proof.add.title',            'ko', '결제 증빙 사진 추가',      'admin', true),
    ('order.proof.add.title',            'en', 'Add payment proof',       'admin', true),
    ('order.proof.add.title',            'th', 'เพิ่มหลักฐานการชำระเงิน',  'admin', true);
