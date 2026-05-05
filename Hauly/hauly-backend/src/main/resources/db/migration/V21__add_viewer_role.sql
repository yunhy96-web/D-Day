-- =============================================================
-- V21__add_viewer_role.sql
-- 방문자용 read-only 역할 VIEWER 추가.
-- 비밀번호 정책 12자 → 4자로 완화 (i18n 메시지 갱신).
-- 실제 user/1234 계정은 InitialUserBootstrapper가 부팅 시 생성.
-- =============================================================

-- VIEWER 역할 코드 등록
INSERT INTO common_code (group_code, code, name_ko, name_en, name_th, sort_order, is_system) VALUES
    ('USER_ROLE', 'VIEWER', '조회 전용', 'Viewer', 'ผู้ชมเท่านั้น', 40, true);

-- 비밀번호 최소 길이 4자로 변경 (백엔드 정책과 일치)
UPDATE i18n_message SET message = '새 비밀번호는 최소 4자 이상이어야 합니다.'
    WHERE message_key = 'pw.error.too_short' AND lang_code = 'ko';
UPDATE i18n_message SET message = 'New password must be at least 4 characters.'
    WHERE message_key = 'pw.error.too_short' AND lang_code = 'en';
UPDATE i18n_message SET message = 'รหัสผ่านใหม่ต้องมีอย่างน้อย 4 ตัวอักษร'
    WHERE message_key = 'pw.error.too_short' AND lang_code = 'th';
