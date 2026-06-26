package com.thaiuniontour.domain.reservation.model;

/** 취소 사유. CUSTOMER=고객취소 / ADMIN=운영취소 / EXPIRED=입금기한 만료 자동취소. */
public enum CancelReason {
    CUSTOMER, ADMIN, EXPIRED
}
