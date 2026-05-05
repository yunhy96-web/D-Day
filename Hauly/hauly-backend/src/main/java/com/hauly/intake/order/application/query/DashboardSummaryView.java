package com.hauly.intake.order.application.query;

import com.hauly.intake.order.domain.model.FulfillmentStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated read model for the dashboard landing page.
 * - totalsByCurrency: SUM(unit_price_amount × quantity) grouped by currency (KRW/THB/USD).
 *   Empty when no items have a recorded price.
 * - totalOrderCount: total number of orders across all statuses.
 * - ordersByFulfillmentStatus: count per fulfillment status (every status key is present,
 *   defaulting to 0 — keeps the chart rendering simple on the frontend).
 * - depositBalanceKrw: current deposit ledger balance (KRW). Negative = customer owes us.
 */
public record DashboardSummaryView(
        Map<String, BigDecimal> totalsByCurrency,
        long totalOrderCount,
        Map<FulfillmentStatus, Long> ordersByFulfillmentStatus,
        BigDecimal depositBalanceKrw,
        /** 재무 입력이 완료된 주문들의 순수익 합계 (KRW). 한 건도 없으면 0. */
        BigDecimal totalNetProfitKrw
) {}
