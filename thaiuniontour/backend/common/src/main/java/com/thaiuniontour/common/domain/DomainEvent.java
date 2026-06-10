package com.thaiuniontour.common.domain;

/**
 * 도메인 이벤트 마커 — Outbox payload로 직렬화되는 이벤트의 공통 계약.
 * 구현체 예: ReservationConfirmed, PaymentCompleted (02-domain.md 이벤트 카탈로그).
 * payload에 schemaVersion을 포함시켜 스키마 진화에 대비한다(확장 seam).
 */
public interface DomainEvent {

    /** 이벤트 타입(Outbox.event_type). 예: "RESERVATION_CONFIRMED". */
    String eventType();

    /** 애그리거트 식별자(Outbox.aggregate_id). */
    String aggregateId();

    /** payload 스키마 버전(진화 대비). 기본 1. */
    default int schemaVersion() {
        return 1;
    }
}
