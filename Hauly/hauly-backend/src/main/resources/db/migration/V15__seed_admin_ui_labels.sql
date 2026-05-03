-- =============================================================
-- V15__seed_admin_ui_labels.sql
-- 어드민 UI에서 하드코딩되어 있던 한글 문자열을 ko/en/th i18n로 마이그레이션.
-- 비밀번호 변경 모달, 이미지 업로더/갤러리, 헤더/사이드바, 주문 삭제 모달 등.
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- ----- 공용 버튼/aria 라벨 -----
    ('btn.close',                 'ko', '닫기',          'admin', true),
    ('btn.close',                 'en', 'Close',         'admin', true),
    ('btn.close',                 'th', 'ปิด',            'admin', true),

    ('btn.prev',                  'ko', '이전',          'admin', true),
    ('btn.prev',                  'en', 'Previous',      'admin', true),
    ('btn.prev',                  'th', 'ก่อนหน้า',      'admin', true),

    ('btn.next',                  'ko', '다음',          'admin', true),
    ('btn.next',                  'en', 'Next',          'admin', true),
    ('btn.next',                  'th', 'ถัดไป',          'admin', true),

    ('btn.deleting',              'ko', '삭제 중…',      'admin', true),
    ('btn.deleting',              'en', 'Deleting…',     'admin', true),
    ('btn.deleting',              'th', 'กำลังลบ…',      'admin', true),

    -- ----- 헤더 / 사이드바 메뉴 -----
    ('menu.language',             'ko', '언어',          'admin', true),
    ('menu.language',             'en', 'Language',      'admin', true),
    ('menu.language',             'th', 'ภาษา',           'admin', true),

    ('menu.password_change',      'ko', '비밀번호 변경',  'admin', true),
    ('menu.password_change',      'en', 'Change Password', 'admin', true),
    ('menu.password_change',      'th', 'เปลี่ยนรหัสผ่าน', 'admin', true),

    ('menu.title',                'ko', '메뉴',          'admin', true),
    ('menu.title',                'en', 'Menu',          'admin', true),
    ('menu.title',                'th', 'เมนู',           'admin', true),

    ('aria.menu_open',            'ko', '메뉴 열기',     'admin', true),
    ('aria.menu_open',            'en', 'Open menu',     'admin', true),
    ('aria.menu_open',            'th', 'เปิดเมนู',       'admin', true),

    ('aria.menu_close',           'ko', '메뉴 닫기',     'admin', true),
    ('aria.menu_close',           'en', 'Close menu',    'admin', true),
    ('aria.menu_close',           'th', 'ปิดเมนู',        'admin', true),

    ('aria.account_menu',         'ko', '계정 메뉴',     'admin', true),
    ('aria.account_menu',         'en', 'Account menu',  'admin', true),
    ('aria.account_menu',         'th', 'เมนูบัญชี',       'admin', true),

    -- ----- 비밀번호 변경 모달 -----
    ('pw.title',                  'ko', '비밀번호 변경',  'admin', true),
    ('pw.title',                  'en', 'Change Password', 'admin', true),
    ('pw.title',                  'th', 'เปลี่ยนรหัสผ่าน', 'admin', true),

    ('pw.help',                   'ko', '현재 비밀번호 확인 후 새 비밀번호로 교체합니다. 최소 12자.', 'admin', true),
    ('pw.help',                   'en', 'Enter your current password, then a new password (min 12 characters).', 'admin', true),
    ('pw.help',                   'th', 'ยืนยันรหัสผ่านปัจจุบันแล้วตั้งรหัสใหม่ (อย่างน้อย 12 ตัวอักษร)', 'admin', true),

    ('pw.success',                'ko', '비밀번호가 변경되었습니다.', 'admin', true),
    ('pw.success',                'en', 'Password changed.',          'admin', true),
    ('pw.success',                'th', 'เปลี่ยนรหัสผ่านสำเร็จแล้ว',  'admin', true),

    ('pw.field.current',          'ko', '현재 비밀번호',  'admin', true),
    ('pw.field.current',          'en', 'Current password', 'admin', true),
    ('pw.field.current',          'th', 'รหัสผ่านปัจจุบัน',  'admin', true),

    ('pw.field.new',              'ko', '새 비밀번호 (12자 이상)', 'admin', true),
    ('pw.field.new',              'en', 'New password (min 12 chars)', 'admin', true),
    ('pw.field.new',              'th', 'รหัสผ่านใหม่ (อย่างน้อย 12 ตัวอักษร)', 'admin', true),

    ('pw.field.confirm',          'ko', '새 비밀번호 확인', 'admin', true),
    ('pw.field.confirm',          'en', 'Confirm new password', 'admin', true),
    ('pw.field.confirm',          'th', 'ยืนยันรหัสผ่านใหม่', 'admin', true),

    ('pw.error.too_short',        'ko', '새 비밀번호는 최소 12자 이상이어야 합니다.', 'admin', true),
    ('pw.error.too_short',        'en', 'New password must be at least 12 characters.', 'admin', true),
    ('pw.error.too_short',        'th', 'รหัสผ่านใหม่ต้องมีอย่างน้อย 12 ตัวอักษร', 'admin', true),

    ('pw.error.mismatch',         'ko', '새 비밀번호가 일치하지 않습니다.', 'admin', true),
    ('pw.error.mismatch',         'en', 'New passwords do not match.',     'admin', true),
    ('pw.error.mismatch',         'th', 'รหัสผ่านใหม่ไม่ตรงกัน',           'admin', true),

    ('pw.error.same_as_current',  'ko', '새 비밀번호가 현재 비밀번호와 같습니다.', 'admin', true),
    ('pw.error.same_as_current',  'en', 'New password is the same as the current password.', 'admin', true),
    ('pw.error.same_as_current',  'th', 'รหัสผ่านใหม่เหมือนรหัสผ่านปัจจุบัน', 'admin', true),

    ('pw.error.failed',           'ko', '비밀번호 변경에 실패했습니다.', 'admin', true),
    ('pw.error.failed',           'en', 'Failed to change password.',    'admin', true),
    ('pw.error.failed',           'th', 'เปลี่ยนรหัสผ่านไม่สำเร็จ',      'admin', true),

    ('pw.btn.change',             'ko', '변경',           'admin', true),
    ('pw.btn.change',             'en', 'Change',         'admin', true),
    ('pw.btn.change',             'th', 'เปลี่ยน',         'admin', true),

    ('pw.btn.changing',           'ko', '변경 중…',       'admin', true),
    ('pw.btn.changing',           'en', 'Changing…',      'admin', true),
    ('pw.btn.changing',           'th', 'กำลังเปลี่ยน…',  'admin', true),

    -- ----- 이미지 업로더 / 갤러리 -----
    ('field.images.label',        'ko', '참고 이미지',     'admin', true),
    ('field.images.label',        'en', 'Reference Images', 'admin', true),
    ('field.images.label',        'th', 'รูปอ้างอิง',       'admin', true),

    ('image.error.max',           'ko', '최대 {{n}}장까지 업로드 가능합니다.', 'admin', true),
    ('image.error.max',           'en', 'You can upload up to {{n}} images.',  'admin', true),
    ('image.error.max',           'th', 'อัปโหลดได้สูงสุด {{n}} รูป',          'admin', true),

    ('image.error.upload_failed', 'ko', '업로드 실패: {{name}} ({{reason}})',  'admin', true),
    ('image.error.upload_failed', 'en', 'Upload failed: {{name}} ({{reason}})', 'admin', true),
    ('image.error.upload_failed', 'th', 'อัปโหลดล้มเหลว: {{name}} ({{reason}})', 'admin', true),

    ('image.aria.zoom',           'ko', '이미지 {{n}} 확대', 'admin', true),
    ('image.aria.zoom',           'en', 'Zoom image {{n}}',  'admin', true),
    ('image.aria.zoom',           'th', 'ขยายรูปที่ {{n}}',   'admin', true),

    -- ----- 주문 삭제 모달 -----
    -- 본문은 \n 으로 줄바꿈 — i18next는 \n을 그대로 출력하지만, window.confirm은 \n을 줄바꿈으로 처리.
    ('order.delete.confirm',      'ko',
        E'주문 {{orderNo}} 을(를) 영구 삭제합니다.\n\n⚠ 이 작업은 복구할 수 없습니다.\n주문 항목, 상태 변경 이력까지 모두 함께 삭제됩니다.\n\n정말 삭제하시겠습니까?',
        'admin', true),
    ('order.delete.confirm',      'en',
        E'Permanently delete order {{orderNo}}.\n\n⚠ This action cannot be undone.\nAll order items and status history will be deleted as well.\n\nAre you sure?',
        'admin', true),
    ('order.delete.confirm',      'th',
        E'ลบคำสั่งซื้อ {{orderNo}} อย่างถาวร\n\n⚠ การกระทำนี้ไม่สามารถย้อนกลับได้\nรายการสินค้าและประวัติการเปลี่ยนสถานะจะถูกลบทั้งหมด\n\nยืนยันการลบหรือไม่?',
        'admin', true),

    ('order.delete.error.with_code', 'ko', '삭제에 실패했습니다 ({{code}}): {{message}}', 'admin', true),
    ('order.delete.error.with_code', 'en', 'Failed to delete ({{code}}): {{message}}',     'admin', true),
    ('order.delete.error.with_code', 'th', 'ลบไม่สำเร็จ ({{code}}): {{message}}',          'admin', true),

    ('order.delete.error.generic', 'ko', '삭제에 실패했습니다. 잠시 후 다시 시도해 주세요.', 'admin', true),
    ('order.delete.error.generic', 'en', 'Failed to delete. Please try again shortly.',     'admin', true),
    ('order.delete.error.generic', 'th', 'ลบไม่สำเร็จ กรุณาลองใหม่อีกครั้ง',                 'admin', true);
