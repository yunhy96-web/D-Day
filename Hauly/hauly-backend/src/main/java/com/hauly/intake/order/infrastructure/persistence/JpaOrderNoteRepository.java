package com.hauly.intake.order.infrastructure.persistence;

import com.hauly.intake.order.domain.model.OrderNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface JpaOrderNoteRepository extends JpaRepository<OrderNote, Long> {

    /** Active (non-deleted) notes for an order, newest first. */
    @Query("""
        SELECT n FROM OrderNote n
        WHERE n.orderId = :orderId AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
        """)
    List<OrderNote> findActiveByOrderId(@Param("orderId") Long orderId);
}
