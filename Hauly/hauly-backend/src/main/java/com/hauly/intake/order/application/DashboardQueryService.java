package com.hauly.intake.order.application;

import com.hauly.intake.deposit.domain.repository.DepositRepository;
import com.hauly.intake.order.application.query.DashboardSummaryView;
import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Read-only aggregation service backing the admin dashboard summary card.
 */
@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final OrderRepository orders;
    private final DepositRepository deposits;

    DashboardQueryService(OrderRepository orders, DepositRepository deposits) {
        this.orders = orders;
        this.deposits = deposits;
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
        BigDecimal totalProfit = orders.findAllWithCompleteFinancials().stream()
                .map(o -> o.getNetProfitKrw())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummaryView(
                orders.sumAmountByCurrency(), total, filled, deposits.currentBalance(), totalProfit);
    }
}
