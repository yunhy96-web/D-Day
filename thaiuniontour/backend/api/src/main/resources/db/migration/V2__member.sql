-- V2: member (회원, Role 기반)
CREATE TABLE member.member (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),                       -- 소셜 확장 시 nullable
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(30),
    role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER'
                CHECK (role IN ('MEMBER','ADMIN')), -- 확장: PARTNER
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE','DORMANT','WITHDRAWN')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
