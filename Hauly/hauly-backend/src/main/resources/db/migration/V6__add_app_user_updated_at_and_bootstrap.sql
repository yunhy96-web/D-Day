-- =============================================================
-- V6__add_app_user_updated_at_and_bootstrap.sql
-- 1. app_user 에 updated_at 컬럼 추가 (V5 에서 누락)
-- 2. role CHECK 제약 추가 (USER_ROLE 그룹 값 반영)
-- 3. email 인덱스 명시적 주석 (UNIQUE 이 자동 인덱스 생성)
-- NOTE: 사용자 시드(bootstrap)는 CommandLineRunner(InitialUserBootstrapper)로 처리
-- =============================================================

-- app_user.updated_at — V1 에서 이미 추가됨 (V5 fix 후 확인)
-- V1 에서 app_user 에 updated_at 이 없었다면 아래 ALTER 필요.
-- V1__init.sql 확인 결과 updated_at 이 이미 있으므로, 이 마이그레이션에서는
-- role CHECK 제약만 추가합니다.

-- role 컬럼 CHECK 제약 추가
-- Postgres 는 common_code 테이블을 직접 CHECK 에 참조할 수 없어 값을 명시합니다.
ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_role
        CHECK (role IN ('INTAKE', 'BUYER', 'ADMIN'));

-- email 에는 UNIQUE 제약이 있어 PostgreSQL 이 자동으로 인덱스를 생성합니다.
-- 별도 CREATE INDEX 불필요. 아래는 문서화 주석입니다.
-- Index: app_user_email_key (auto-created by UNIQUE constraint on email)
