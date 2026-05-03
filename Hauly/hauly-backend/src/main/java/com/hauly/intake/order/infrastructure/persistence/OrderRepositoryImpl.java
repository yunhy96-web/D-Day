package com.hauly.intake.order.infrastructure.persistence;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderStatusLog;
import com.hauly.intake.order.domain.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter that satisfies the domain {@link OrderRepository} interface using Spring Data JPA
 * delegates. Keeps Spring Data types out of the domain layer.
 */
@Component
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderEntityRepository orders;
    private final JpaOrderStatusLogRepository logs;

    public OrderRepositoryImpl(JpaOrderEntityRepository orders, JpaOrderStatusLogRepository logs) {
        this.orders = orders;
        this.logs = logs;
    }

    @Override
    public Order save(Order order) {
        return orders.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orders.findById(id);
    }

    @Override
    public Page<Order> findAllByFilter(FulfillmentStatus status, String q, Pageable pageable) {
        return orders.findAllByFilter(status, q, pageable);
    }

    @Override
    public Page<Order> findAllByFilterSortByProductName(FulfillmentStatus status, String q, Pageable pageable) {
        return orders.findAllByFilterSortByProductName(status, q, pageable);
    }

    @Override
    public List<OrderStatusLog> findStatusLogsByOrderId(Long orderId) {
        return logs.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    @Override
    public OrderStatusLog saveStatusLog(OrderStatusLog log) {
        return logs.save(log);
    }

    @Override
    public void deleteOrderAndLogs(Long orderId) {
        // order_status_log has no ON DELETE CASCADE — clear the log rows first.
        // Use bulk JPQL DELETE on the order so Hibernate doesn't lazy-load the items
        // collection just to cascade-remove rows the FK is already going to drop.
        logs.deleteByOrderId(orderId);
        orders.deleteOrderById(orderId);
    }

    @Override
    public Map<String, BigDecimal> sumAmountByCurrency() {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Object[] row : orders.sumAmountByCurrency()) {
            totals.put((String) row[0], (BigDecimal) row[1]);
        }
        return totals;
    }

    @Override
    public Map<FulfillmentStatus, Long> countByFulfillmentStatus() {
        Map<FulfillmentStatus, Long> map = new EnumMap<>(FulfillmentStatus.class);
        for (Object[] row : orders.countByFulfillmentStatus()) {
            map.put((FulfillmentStatus) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
