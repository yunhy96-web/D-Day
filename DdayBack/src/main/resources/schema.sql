-- Articles 테이블
DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles (
    no BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(255) NOT NULL,
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
    password VARCHAR(255),
    role VARCHAR(255) CHECK (role IN ('USER', 'ADMIN')),
    timezone VARCHAR(255),
    refresh_token VARCHAR(255),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

CREATE INDEX idx_users_uuid ON users(uuid);
