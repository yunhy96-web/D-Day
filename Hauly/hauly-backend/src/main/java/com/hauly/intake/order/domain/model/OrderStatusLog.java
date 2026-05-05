package com.hauly.intake.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Append-only audit log of every status transition on an Order.
 * One row per dimension change. Created by the Order aggregate when status mutates.
 */
@Entity
@Table(name = "order_status_log")
public class OrderStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatusDimension dimension;

    @Column(name = "from_code", length = 32)
    private String fromCode;

    @Column(name = "to_code", nullable = false, length = 32)
    private String toCode;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** True when this transition was an ADMIN-forced override that bypassed the state machine. */
    @Column(name = "forced", nullable = false)
    private boolean forced;

    /** JPA only. */
    protected OrderStatusLog() {}

    public static OrderStatusLog record(Long orderId, StatusDimension dimension, String fromCode,
                                        String toCode, Long changedBy, String note) {
        return new OrderStatusLog(orderId, dimension, fromCode, toCode, changedBy, note, false);
    }

    /** Records a forced (state-machine-bypassing) transition. Reason is mandatory upstream. */
    public static OrderStatusLog recordForced(Long orderId, StatusDimension dimension, String fromCode,
                                              String toCode, Long changedBy, String reason) {
        return new OrderStatusLog(orderId, dimension, fromCode, toCode, changedBy, reason, true);
    }

    private OrderStatusLog(Long orderId, StatusDimension dimension, String fromCode, String toCode,
                           Long changedBy, String note, boolean forced) {
        this.orderId = orderId;
        this.dimension = dimension;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.changedBy = changedBy;
        this.note = note;
        this.forced = forced;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public StatusDimension getDimension() { return dimension; }
    public String getFromCode() { return fromCode; }
    public String getToCode() { return toCode; }
    public Long getChangedBy() { return changedBy; }
    public String getNote() { return note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isForced() { return forced; }
}
