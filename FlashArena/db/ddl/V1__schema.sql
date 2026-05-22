-- =============================================================
-- FlashArena :: V1 - 스키마(논리적 도메인) 생성
-- -------------------------------------------------------------
-- 단일 물리 PostgreSQL 안에서 MSA처럼 도메인을 격리하기 위해
-- auth / order / payment 세 개의 스키마를 만든다.
--
-- ⚠️ 제약 (반드시 준수):
--   - 스키마 간 물리 FK 금지 (UUID 데이터타입으로만 참조)
--   - 스키마 간 JOIN / 단일 트랜잭션 공통 처리 금지
--
-- ⚠️ "order"는 SQL 예약어다. 스키마/테이블 참조 시 항상 큰따옴표로 감싼다.
--    예) "order".product, "order".orders
--    JPA에서는 @Table(schema = "\"order\"") 또는
--    hibernate.globally_quoted_identifiers=true 로 처리한다.
--
-- gen_random_uuid()는 PostgreSQL 13+ 코어에 내장되어 별도 확장 불필요
-- (현재 이미지: postgres:15-alpine).
-- =============================================================

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS "order";
CREATE SCHEMA IF NOT EXISTS payment;

COMMENT ON SCHEMA auth      IS 'FlashArena 인증 도메인 (JWT 발급/검증)';
COMMENT ON SCHEMA "order"   IS 'FlashArena 주문/재고 도메인 (동시성 시뮬레이터 핵심)';
COMMENT ON SCHEMA payment   IS 'FlashArena 결제 도메인 (Transactional Outbox / 멱등성)';
