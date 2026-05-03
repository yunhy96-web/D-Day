package com.hauly.intake.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Collaborative note attached to an Order. Both INTAKE operators (myself and partner)
 * can leave notes that act like an internal threaded comment log.
 *
 * Edit and delete are restricted to the original author. Deletion is soft so the
 * audit trail remains intact for incident review.
 */
@Entity
@Table(name = "order_note")
public class OrderNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /** JPA only. */
    protected OrderNote() {}

    public static OrderNote create(Long orderId, Long authorId, String body) {
        OffsetDateTime now = OffsetDateTime.now();
        OrderNote note = new OrderNote();
        note.orderId = orderId;
        note.authorId = authorId;
        note.body = body;
        note.createdAt = now;
        note.updatedAt = now;
        return note;
    }

    /**
     * Update the note body. Caller must verify author ownership before invoking this.
     */
    public void edit(String body) {
        this.body = body;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Mark as deleted. Soft-delete preserves the row for audit; queries must filter
     * {@code deleted_at IS NULL} to hide it from users.
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAuthor(Long userId) {
        return userId != null && userId.equals(this.authorId);
    }

    /** True if {@link #updatedAt} is meaningfully after {@link #createdAt}. */
    public boolean wasEdited() {
        // 1-second tolerance — DB clocks and JVM clocks may differ slightly on insert.
        return updatedAt != null && createdAt != null
                && updatedAt.toEpochSecond() - createdAt.toEpochSecond() > 1;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
}
