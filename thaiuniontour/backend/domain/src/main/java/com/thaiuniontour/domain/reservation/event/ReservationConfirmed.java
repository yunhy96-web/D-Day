package com.thaiuniontour.domain.reservation.event;

import com.thaiuniontour.common.domain.DomainEvent;
import java.util.UUID;

/** 예약 확정(입금승인) 이벤트 → Outbox payload. 결제 도메인이 구독해 결제이력을 멱등 적재. */
public record ReservationConfirmed(
        UUID reservationId,
        UUID memberId,
        long amount,
        String paymentMethod
) implements DomainEvent {

    public static final String TYPE = "RESERVATION_CONFIRMED";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public String aggregateId() {
        return reservationId.toString();
    }
}
