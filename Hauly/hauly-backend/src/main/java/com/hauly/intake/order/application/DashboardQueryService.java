package com.hauly.intake.order.application;

import com.hauly.intake.order.application.query.DashboardSummaryView;
import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/**
 * Read-only aggregation service backing the admin dashboard summary card.
 */
@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final OrderRepository orders;

    DashboardQueryService(OrderRepository orders) {
        this.orders = orders;
    }

    public DashboardSummaryView summary() {
        Map<FulfillmentStatus, Long> raw = orders.countByFulfillmentStatus();
        Map<FulfillmentStatus, Long> filled = new EnumMap<>(FulfillmentStatus.class);
        long total = 0L;
        for (FulfillmentStatus s : FulfillmentStatus.values()) {
            long count = raw.getOrDefault(s, 0L);
            filled.put(s, count);
            total += count;
        }
        return new DashboardSummaryView(orders.sumAmountByCurrency(), total, filled);
    }
}
