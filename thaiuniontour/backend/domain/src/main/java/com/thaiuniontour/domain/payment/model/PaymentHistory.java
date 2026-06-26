package com.thaiuniontour.domain.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 이력 = Inbox(소비자 멱등). reservation_id UNIQUE가 중복결제 최종 방어선(exactly-once 효과).
 */
@Entity
@Table(schema = "payment", name = "payment_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "reservation_id", columnDefinition = "uuid", nullable = false)
    private UUID reservationId;

    @Column(name = "member_id", columnDefinition = "uuid")
    private UUID memberId;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "paid_at", insertable = false, updatable = false)
    private OffsetDateTime paidAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static PaymentHistory paid(UUID reservationId, UUID memberId, String method, long amount) {
        PaymentHistory p = new PaymentHistory();
        p.id = UUID.randomUUID();
        p.reservationId = reservationId;
        p.memberId = memberId;
        p.method = method;
        p.amount = amount;
        p.status = "PAID";
        return p;
    }
}
