package com.hauly.intake.order.infrastructure.persistence;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data interface for the Order JPA entity.
 * Wrapped by {@link OrderRepositoryImpl} so the domain layer never sees Spring Data types.
 */
@Repository
interface JpaOrderEntityRepository extends JpaRepository<Order, Long> {

    /**
     * List with optional status filter and free-text search across orderNo / customer name /
     * product name. Sort is applied via {@link Pageable} so callers can request any column
     * on the Order entity (createdAt, fulfillmentStatus, orderNo, …).
     */
    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.fulfillmentStatus = :status)
              AND (
                  COALESCE(:q, '') = '' OR
                  LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  EXISTS (SELECT 1 FROM OrderItem i WHERE i.order = o
                          AND LOWER(i.productName) LIKE LOWER(CONCAT('%', :q, '%'))) OR
                  EXISTS (SELECT 1 FROM com.hauly.shared.customer.domain.model.Customer c
                          WHERE c.id = o.customerId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
              )
            """)
    Page<Order> findAllByFilter(@Param("status") FulfillmentStatus status,
                                @Param("q") String q,
                                Pageable pageable);

    /**
     * productName lives on the OrderItem child, not Order, so Pageable's sort can't reach it.
     * Use a correlated subquery to sort by the order's first item name instead.
     * Direction is fixed ASC — a separate method with DESC can be added later if needed.
     *
     * Spring Data can't derive a count query for an ORDER BY with a correlated subquery,
     * so we supply one explicitly via {@code countQuery}.
     */
    @Query(value = """
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.fulfillmentStatus = :status)
              AND (
                  COALESCE(:q, '') = '' OR
                  LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  EXISTS (SELECT 1 FROM OrderItem i WHERE i.order = o
                          AND LOWER(i.productName) LIKE LOWER(CONCAT('%', :q, '%'))) OR
                  EXISTS (SELECT 1 FROM com.hauly.shared.customer.domain.model.Customer c
                          WHERE c.id = o.customerId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
              )
            ORDER BY (SELECT MIN(i.productName) FROM OrderItem i WHERE i.order = o) ASC
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            WHERE (:status IS NULL OR o.fulfillmentStatus = :status)
              AND (
                  COALESCE(:q, '') = '' OR
                  LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  EXISTS (SELECT 1 FROM OrderItem i WHERE i.order = o
                          AND LOWER(i.productName) LIKE LOWER(CONCAT('%', :q, '%'))) OR
                  EXISTS (SELECT 1 FROM com.hauly.shared.customer.domain.model.Customer c
                          WHERE c.id = o.customerId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
              )
            """)
    Page<Order> findAllByFilterSortByProductName(@Param("status") FulfillmentStatus status,
                                                 @Param("q") String q,
                                                 Pageable pageable);

    /**
     * Sum of unit_price_amount × quantity, grouped by currency. Items without a currency
     * (the legacy/empty rows allowed by the V10 CHECK constraint) are skipped.
     * Returns Object[]{currency, total} tuples — caller flattens into a Map.
     */
    @Query("""
            SELECT i.unitPriceCurrency, SUM(i.unitPriceAmount * i.quantity)
            FROM OrderItem i
            WHERE i.unitPriceCurrency IS NOT NULL AND i.unitPriceAmount IS NOT NULL
            GROUP BY i.unitPriceCurrency
            ORDER BY i.unitPriceCurrency
            """)
    List<Object[]> sumAmountByCurrency();

    /**
     * Order count per fulfillment status. Returns tuples {status, count}.
     */
    @Query("""
            SELECT o.fulfillmentStatus, COUNT(o)
            FROM Order o
            GROUP BY o.fulfillmentStatus
            """)
    List<Object[]> countByFulfillmentStatus();

    /**
     * Bulk DELETE that bypasses Hibernate's lifecycle so the items collection isn't
     * lazy-loaded and individually removed. order_item rows are cleaned by the FK's
     * ON DELETE CASCADE; order_status_log rows must be deleted separately first.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Order o WHERE o.id = :id")
    int deleteOrderById(@Param("id") Long id);

    /**
     * Resolves order_no for a batch of ids — used by the deposit ledger view to label
     * PURCHASE / REFUND rows without N+1 lookups.
     */
    @Query("SELECT o.id, o.orderNo FROM Order o WHERE o.id IN :ids")
    List<Object[]> findOrderNosByIds(@Param("ids") Collection<Long> ids);

    /**
     * 순수익 계산 가능한 주문들 — 핵심 2개(실결제금액 + 매출)만 NOT NULL. 물류비는 미입력 시 0으로 간주.
     * CANCELLED 제외. 환율(THB→KRW 변환) 조건은 도메인 레이어에서 처리.
     */
    @Query("""
            SELECT o FROM Order o
            WHERE o.paidAmountKrw IS NOT NULL
              AND o.customerRevenueAmount IS NOT NULL
              AND o.fulfillmentStatus <> com.hauly.intake.order.domain.model.FulfillmentStatus.CANCELLED
            """)
    List<Order> findAllWithCompleteFinancials();
}
