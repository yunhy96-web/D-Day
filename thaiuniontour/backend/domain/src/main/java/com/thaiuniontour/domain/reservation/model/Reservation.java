package com.thaiuniontour.domain.reservation.model;

import com.thaiuniontour.common.domain.AggregateRoot;
import com.thaiuniontour.common.error.ErrorCode;
import com.thaiuniontour.common.exception.BusinessException;
import com.thaiuniontour.domain.reservation.event.ReservationCancelled;
import com.thaiuniontour.domain.reservation.event.ReservationConfirmed;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 애그리거트 루트 (전술 DDD). 불변식: CONFIRMED만 실제확정 / CANCELLED는 재취소 불가.
 * 행위(confirm/cancel)는 도메인 이벤트를 등록하고, 애플리케이션 서비스가 Outbox로 옮긴다.
 */
@Entity
@Table(schema = "reservation", name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends AggregateRoot {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "member_id", columnDefinition = "uuid")
    private UUID memberId;                       // nullable=게스트

    @Column(name = "product_id", columnDefinition = "uuid", nullable = false)
    private UUID productId;

    @Column(name = "product_schedule_id", columnDefinition = "uuid", nullable = false)
    private UUID productScheduleId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "reserver_name", nullable = false)
    private String reserverName;
    @Column(name = "reserver_email", nullable = false)
    private String reserverEmail;
    @Column(name = "reserver_phone", nullable = false)
    private String reserverPhone;

    @Column(name = "reservation_no", nullable = false)
    private String reservationNo;

    @Column(name = "lookup_token", columnDefinition = "uuid", nullable = false)
    private UUID lookupToken;

    @Column(name = "payment_deadline")
    private OffsetDateTime paymentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason")
    private CancelReason cancelReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /** 예약 생성(PENDING). reservationNo/deadline는 서비스가 계산해 전달(유일성/정책). */
    public static Reservation create(UUID memberId, UUID productId, UUID productScheduleId,
                                     LocalDate saleDate, int quantity, long amount, String paymentMethod,
                                     String reserverName, String reserverEmail, String reserverPhone,
                                     String reservationNo, OffsetDateTime paymentDeadline) {
        Reservation r = new Reservation();
        r.id = UUID.randomUUID();
        r.memberId = memberId;
        r.productId = productId;
        r.productScheduleId = productScheduleId;
        r.saleDate = saleDate;
        r.quantity = quantity;
        r.amount = amount;
        r.status = ReservationStatus.PENDING;
        r.paymentMethod = paymentMethod;
        r.reserverName = reserverName;
        r.reserverEmail = reserverEmail;
        r.reserverPhone = reserverPhone;
        r.reservationNo = reservationNo;
        r.lookupToken = UUID.randomUUID();
        r.paymentDeadline = paymentDeadline;
        return r;
    }

    /** 입금 확인(admin 수동승인) → 실제 확정 + ReservationConfirmed 이벤트. */
    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_STATE, "결제대기 상태만 승인할 수 있습니다.");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now();
        registerEvent(new ReservationConfirmed(id, memberId, amount, paymentMethod));
    }

    /** 취소/만료 → 재고 복원/환불 보상 이벤트. */
    public void cancel(CancelReason reason) {
        if (status == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_STATE, "이미 취소된 예약입니다.");
        }
        boolean wasConfirmed = (status == ReservationStatus.CONFIRMED);
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = OffsetDateTime.now();
        this.cancelReason = reason;
        registerEvent(new ReservationCancelled(id, productScheduleId, quantity, reason.name(), wasConfirmed));
    }

    @Transient
    public boolean isExpired(OffsetDateTime now) {
        return status == ReservationStatus.PENDING
                && paymentDeadline != null && paymentDeadline.isBefore(now);
    }
}
