package com.hauly.intake.order.infrastructure.persistence;

import com.hauly.intake.order.domain.model.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface JpaOrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {

    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    @Modifying
    @Query("DELETE FROM OrderStatusLog l WHERE l.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}
