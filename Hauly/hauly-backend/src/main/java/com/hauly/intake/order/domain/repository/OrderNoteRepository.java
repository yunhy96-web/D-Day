package com.hauly.intake.order.domain.repository;

import com.hauly.intake.order.domain.model.OrderNote;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository for {@link OrderNote}. OrderNote is its own aggregate root —
 * notes outlive individual edits to the parent Order and are queried independently.
 */
public interface OrderNoteRepository {

    OrderNote save(OrderNote note);

    Optional<OrderNote> findById(Long id);

    /** Active (non-soft-deleted) notes for an order, ordered newest first. */
    List<OrderNote> findActiveByOrderId(Long orderId);
}
