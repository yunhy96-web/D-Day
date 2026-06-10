/**
 * Catalog Bounded Context (경량 → 필요시 DDD 승격). 상품·분류(카테고리 트리)·날짜별 재고·검색.
 *
 * <p>4계층 패키지 컨벤션(컨텍스트별 동일):
 * <ul>
 *   <li>{@code model}      — 엔티티/VO</li>
 *   <li>{@code repository} — Spring Data JPA 리포지토리(포트)</li>
 *   <li>{@code service}    — 도메인 서비스</li>
 *   <li>{@code port}       — 확장 전략 포트(어댑터로 무수정 교체)</li>
 * </ul>
 *
 * <p>엔티티는 {@code @Table(schema = "catalog")} 명시 필수(전역 default_schema 미사용).
 */
package com.thaiuniontour.domain.catalog;
