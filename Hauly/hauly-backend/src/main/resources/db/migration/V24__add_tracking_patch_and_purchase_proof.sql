-- =============================================================
-- V24__add_tracking_patch_and_purchase_proof.sql
-- 1) 주문 PURCHASED 후 캡처한 결제 증빙 이미지 키 목록.
-- 2) i18n 라벨 (트래킹 수정 / 결제 증빙 업로드).
-- (트래킹 수정 endpoint는 코드 레벨 변경, DB 변경 없음)
-- =============================================================

ALTER TABLE "order"
    ADD COLUMN purchase_proof_keys JSONB;

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('order.tracking.edit.button',  'ko', '배송 정보 수정',         'admin', true),
    ('order.tracking.edit.button',  'en', 'Edit shipping info',     'admin', true),
    ('order.tracking.edit.button',  'th', 'แก้ไขข้อมูลจัดส่ง',       'admin', true),

    ('order.tracking.edit.title',   'ko', '배송 정보 수정',         'admin', true),
    ('order.tracking.edit.title',   'en', 'Edit shipping info',     'admin', true),
    ('order.tracking.edit.title',   'th', 'แก้ไขข้อมูลจัดส่ง',       'admin', true),

    ('order.tracking.edit.save',    'ko', '저장',                   'admin', true),
    ('order.tracking.edit.save',    'en', 'Save',                   'admin', true),
    ('order.tracking.edit.save',    'th', 'บันทึก',                 'admin', true),

    ('order.purchased.modal.proof', 'ko', '결제 증빙 사진 (선택)',  'admin', true),
    ('order.purchased.modal.proof', 'en', 'Payment proof (optional)','admin', true),
    ('order.purchased.modal.proof', 'th', 'หลักฐานการชำระเงิน (เลือกเสริม)', 'admin', true),

    ('order.detail.proof.title',    'ko', '결제 증빙',              'admin', true),
    ('order.detail.proof.title',    'en', 'Payment proof',          'admin', true),
    ('order.detail.proof.title',    'th', 'หลักฐานการชำระเงิน',     'admin', true);
