package com.thaiuniontour.domain.reservation.model;

/** 예약 상태. PENDING=임시확보(입금대기) / CONFIRMED=실제확정(입금승인) / CANCELLED. */
public enum ReservationStatus {
    PENDING, CONFIRMED, CANCELLED
}
