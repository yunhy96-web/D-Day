-- V1: 스키마 & common (공통코드/라벨)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SCHEMA IF NOT EXISTS common;
CREATE SCHEMA IF NOT EXISTS member;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS reservation;
CREATE SCHEMA IF NOT EXISTS payment;

-- 공통코드: 업무 확장값(결제수단 등). 상태값은 도메인 CHECK로 관리(여기 두지 않음).
CREATE TABLE common.common_code (
    code_group  VARCHAR(50)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    use_yn      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (code_group, code)
);

-- 다국어 라벨: code+locale 유일. locale = ko|en.
CREATE TABLE common.label (
    code    VARCHAR(100) NOT NULL,
    locale  VARCHAR(5)   NOT NULL CHECK (locale IN ('ko','en')),
    text    VARCHAR(500) NOT NULL,
    PRIMARY KEY (code, locale)
);
