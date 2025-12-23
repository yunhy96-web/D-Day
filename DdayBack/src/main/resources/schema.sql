-- Articles 테이블
DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles (
    no BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
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
