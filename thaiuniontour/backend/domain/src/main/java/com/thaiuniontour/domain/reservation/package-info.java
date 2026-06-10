/**
 * Reservation Bounded Context (전술 DDD). 예약 애그리거트 + Transactional Outbox(생산자).
 *
 * <p>핵심 불변식: 재고 0이면 예약 불가 / CONFIRMED만 취소 / 금액·통화(KRW) 일관.
 * <p>예약 생성 트랜잭션 = 재고 원자차감 + reservation INSERT + outbox INSERT (원자 커밋).
 * <p>좌석 임시점유(hold/TTL)는 status에 HELD 추가로 확장(seam #6).
 * <p>4계층 컨벤션. 엔티티는 {@code @Table(schema = "reservation")} 명시 필수.
 */
package com.thaiuniontour.domain.reservation;
