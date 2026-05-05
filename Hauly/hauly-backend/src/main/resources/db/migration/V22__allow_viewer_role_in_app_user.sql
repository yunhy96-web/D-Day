-- =============================================================
-- V22__allow_viewer_role_in_app_user.sql
-- V6에서 만든 chk_app_user_role 체크 제약(INTAKE/BUYER/ADMIN)에
-- VIEWER를 추가하여 부트스트래퍼의 user 계정 생성을 허용한다.
-- =============================================================

ALTER TABLE app_user
    DROP CONSTRAINT IF EXISTS chk_app_user_role;
ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_role
        CHECK (role IN ('INTAKE', 'BUYER', 'ADMIN', 'VIEWER'));
