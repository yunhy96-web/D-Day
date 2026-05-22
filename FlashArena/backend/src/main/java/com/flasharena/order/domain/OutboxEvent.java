package com.flasharena.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * "order".outbox 매핑 엔티티 (Transactional Outbox).
 * 핵심: orders INSERT 와 이 outbox INSERT 가 같은 order-스키마 로컬 트랜잭션에서 원자적으로 커밋된다.
 * → 교차 스키마 트랜잭션 / Redis dual-write 없이 이벤트 유실 0%.
 * payload 는 JSONB 컬럼 → Hibernate 6 @JdbcTypeCode(JSON) 로 String 에 매핑(round-trip 검증됨).
 * created_at 은 DB default(now()) 가 채우므로 쓰기를 막는다.
 */
@Entity
@Table(name = "outbox", schema = "order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    public static final String STATUS_READY = "READY";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    // order id 를 "타입으로만" 참조 (물리 FK 없음).
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    // READY → 발행 대상 / PUBLISHED → Redis(XADD) 적재 완료 / FAILED → 재시도 초과(준-DLQ)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    private OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload, String status) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = 0;
    }

    /** 주문 성공(ORDER_COMPLETED) 이벤트를 READY 상태로 생성하는 팩토리. 주문 트랜잭션 안에서 호출한다. */
    public static OutboxEvent orderCompleted(UUID orderId, String payloadJson) {
        return new OutboxEvent("ORDER", orderId, "ORDER_COMPLETED", payloadJson, STATUS_READY);
    }
}
