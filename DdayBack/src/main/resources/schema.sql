-- Articles 테이블
DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles (
    no BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    topic VARCHAR(50),
    article_type VARCHAR(20) DEFAULT 'NORMAL',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    original_lang VARCHAR(10),
    title_ko VARCHAR(255),
    content_ko TEXT,
    title_th VARCHAR(255),
    content_th TEXT,
    translation_status VARCHAR(20) DEFAULT 'PENDING',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

CREATE INDEX idx_articles_uuid ON articles(uuid);
CREATE INDEX idx_articles_topic ON articles(topic);
CREATE INDEX idx_articles_article_type ON articles(article_type);

-- Users 테이블
DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE users (
    no BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    role VARCHAR(255) CHECK (role IN ('USER', 'ADMIN', 'DEV')),
    timezone VARCHAR(255),
    refresh_token VARCHAR(255),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

CREATE INDEX idx_users_uuid ON users(uuid);

-- Common Codes 테이블 (공통코드)
DROP TABLE IF EXISTS common_codes CASCADE;
CREATE TABLE common_codes (
    no BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    group_code VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    label_ko VARCHAR(100) NOT NULL,
    label_en VARCHAR(100) NOT NULL,
    label_th VARCHAR(100) NOT NULL,
    sort_order INT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_code, code)
);

CREATE INDEX idx_common_codes_group ON common_codes(group_code);

-- 공통코드 초기 데이터
INSERT INTO common_codes (group_code, code, label_ko, label_en, label_th, sort_order) VALUES
('ARTICLE_TOPIC', 'GENERAL', '일반', 'General', 'ทั่วไป', 1),
('ARTICLE_TOPIC', 'TECH', '기술', 'Technology', 'เทคโนโลยี', 2),
('ARTICLE_TOPIC', 'LIFE', '일상', 'Life', 'ชีวิต', 3),
('ARTICLE_TOPIC', 'TRAVEL', '여행', 'Travel', 'การท่องเที่ยว', 4),
('ARTICLE_TOPIC', 'FOOD', '음식', 'Food', 'อาหาร', 5),
('ARTICLE_TOPIC', 'CULTURE', '문화', 'Culture', 'วัฒนธรรม', 6),
('ARTICLE_TYPE', 'NORMAL', '일반글', 'Normal', 'ทั่วไป', 1),
('ARTICLE_TYPE', 'SECRET', '비밀글', 'Secret', 'ลับ', 2);

-- Role Article Permissions 테이블 (권한별 게시글 타입 접근 권한)
DROP TABLE IF EXISTS role_article_permissions CASCADE;
CREATE TABLE role_article_permissions (
    no BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    role VARCHAR(20) NOT NULL,
    article_type VARCHAR(20) NOT NULL,
    can_read BOOLEAN DEFAULT true,
    can_write BOOLEAN DEFAULT true,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role, article_type)
);

-- 권한 초기 데이터
INSERT INTO role_article_permissions (role, article_type, can_read, can_write) VALUES
('DEV', 'NORMAL', true, true),
('DEV', 'SECRET', true, true),
('ADMIN', 'NORMAL', true, true),
('ADMIN', 'SECRET', true, true),
('USER', 'NORMAL', true, true),
('USER', 'SECRET', false, false);
