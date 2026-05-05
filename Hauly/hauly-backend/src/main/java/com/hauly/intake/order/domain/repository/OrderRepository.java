package com.hauly.intake.order.domain.repository;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderStatusLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Order repository — domain interface.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Page<Order> findAllByFilter(FulfillmentStatus status, String q, Pageable pageable);

    Page<Order> findAllByFilterSortByProductName(FulfillmentStatus status, String q, Pageable pageable);

    List<OrderStatusLog> findStatusLogsByOrderId(Long orderId);

    OrderStatusLog saveStatusLog(OrderStatusLog log);

    /** Hard-delete an order along with its items (CASCADE) and status log rows. Irreversible. */
    void deleteOrderAndLogs(Long orderId);

    /** SUM(unit_price_amount × quantity) grouped by currency, items with currency only. */
    Map<String, BigDecimal> sumAmountByCurrency();

    /** Order count grouped by fulfillment status. Statuses with zero rows are absent. */
    Map<FulfillmentStatus, Long> countByFulfillmentStatus();

    /** Bulk lookup of order_no by id — for cross-aggregate read views (e.g. deposit ledger). */
    Map<Long, String> findOrderNosByIds(Collection<Long> ids);

    /**
     * 재무 입력이 완료된 주문들 (customer_revenue/logistics 양쪽 + paid_amount 모두 NOT NULL,
     * THB값 있을 시 환율도 NOT NULL). 순수익 합산용.
     */
    List<Order> findAllWithCompleteFinancials();
}
