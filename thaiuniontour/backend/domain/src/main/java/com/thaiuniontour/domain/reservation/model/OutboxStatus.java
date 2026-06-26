package com.thaiuniontour.domain.reservation.model;

/** Outbox 발행 상태. READY=발행대기 / PUBLISHED=스트림 적재완료 / FAILED=재시도초과(준-DLQ). */
public enum OutboxStatus {
    READY, PUBLISHED, FAILED
}
