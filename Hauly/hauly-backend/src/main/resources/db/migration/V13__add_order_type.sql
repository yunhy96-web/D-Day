-- =============================================================
-- V13__add_order_type.sql
-- 주문 타입 컬럼 추가: 개별 주문(기본) / 세트 주문(2+1 같은 묶음).
-- 기존 주문은 모두 INDIVIDUAL로 백필. CHECK 제약으로 값 제한.
-- =============================================================

ALTER TABLE "order"
    ADD COLUMN order_type VARCHAR(16) NOT NULL DEFAULT 'INDIVIDUAL';

ALTER TABLE "order"
    ADD CONSTRAINT chk_order_type
        CHECK (order_type IN ('INDIVIDUAL', 'SET'));
