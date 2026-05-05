-- =============================================================
-- V27__add_user_preferred_language.sql
-- 계정별 디폴트 언어. 헤더 언어 드롭다운에서 선택 시 자동 저장하여
-- 다음 로그인/페이지 새로고침 시 동일한 언어로 자동 복원.
-- NULL = 미설정 (전역 디폴트인 'ko'를 사용).
-- =============================================================

ALTER TABLE app_user
    ADD COLUMN preferred_language VARCHAR(2);

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_lang
    CHECK (preferred_language IS NULL OR preferred_language IN ('ko','en','th'));
