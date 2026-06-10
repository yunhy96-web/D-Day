/**
 * Payment Bounded Context (전술 DDD). 결제 처리·멱등 이력(Inbox).
 *
 * <p>핵심 불변식: 예약당 결제 1회(멱등) — payment_history {@code UNIQUE(reservation_id)}.
 * <p>결제 도메인은 reservation 스키마/Outbox를 직접 조회하지 않고 <b>이벤트만 구독</b>한다.
 * <p>4계층 컨벤션 + {@code port}(PaymentGateway·PaymentConfirmation 확장 seam).
 * <p>엔티티는 {@code @Table(schema = "payment")} 명시 필수.
 */
package com.thaiuniontour.domain.payment;
