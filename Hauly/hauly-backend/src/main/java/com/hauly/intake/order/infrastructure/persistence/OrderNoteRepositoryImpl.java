package com.hauly.intake.order.infrastructure.persistence;

import com.hauly.intake.order.domain.model.OrderNote;
import com.hauly.intake.order.domain.repository.OrderNoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that exposes {@link OrderNoteRepository} on top of Spring Data JPA.
 */
@Component
public class OrderNoteRepositoryImpl implements OrderNoteRepository {

    private final JpaOrderNoteRepository notes;

    public OrderNoteRepositoryImpl(JpaOrderNoteRepository notes) {
        this.notes = notes;
    }

    @Override
    public OrderNote save(OrderNote note) {
        return notes.save(note);
    }

    @Override
    public Optional<OrderNote> findById(Long id) {
        return notes.findById(id);
    }

    @Override
    public List<OrderNote> findActiveByOrderId(Long orderId) {
        return notes.findActiveByOrderId(orderId);
    }
}
