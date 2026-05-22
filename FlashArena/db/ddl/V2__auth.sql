-- =============================================================
-- FlashArena :: V2 - auth 스키마
-- -------------------------------------------------------------
-- 회원/인증 도메인. 무단 트래픽 방지를 위해 회원가입 없이
-- 고정 공용 계정(user / 1234)으로만 로그인한다.
-- =============================================================

CREATE TABLE IF NOT EXISTS auth.users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(100) NOT NULL,            -- BCrypt 해시 (앱에서 인코딩)
    role          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_ADMIN',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 공용 계정 1개만 존재하도록 username 유니크
CREATE UNIQUE INDEX IF NOT EXISTS uq_auth_users_username
    ON auth.users (username);

COMMENT ON TABLE  auth.users          IS '공용 관리자 계정 (시뮬레이터 접근용)';
COMMENT ON COLUMN auth.users.password IS 'BCrypt 해시. 평문 저장 금지';
COMMENT ON COLUMN auth.users.role     IS 'JWT 클레임에 실릴 권한 (예: ROLE_ADMIN)';

-- 주의: 공용 계정(user/1234) 시딩은 BCrypt 인코딩이 필요하므로
--       Phase 2에서 Spring 의 CommandLineRunner 로 1회 삽입한다.
--       (DDL 에 평문/임의 해시를 박지 않는다)
