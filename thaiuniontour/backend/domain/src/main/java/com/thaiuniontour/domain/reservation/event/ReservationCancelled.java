package com.thaiuniontour.domain.reservation.event;

import com.thaiuniontour.common.domain.DomainEvent;
import java.util.UUID;

/** 예약 취소/만료 이벤트 → Outbox payload. Catalog(재고복원)·Payment(환불) 구독. */
public record ReservationCancelled(
        UUID reservationId,
        UUID productScheduleId,
        int quantity,
        String reason,
        boolean wasConfirmed
) implements DomainEvent {

    public static final String TYPE = "RESERVATION_CANCELLED";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public String aggregateId() {
        return reservationId.toString();
    }
}
