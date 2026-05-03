package com.hauly.intake.order.application.query;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderItem;
import com.hauly.intake.order.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read model for the order list page.
 * totalsByCurrency: per-currency sum of unit_price_amount × quantity, keyed by ISO code.
 * Items without a currency are excluded; an order with no priced items returns an empty map.
 */
public record OrderListItemView(
        Long id,
        String orderNo,
        Long customerId,
        String customerName,
        FulfillmentStatus fulfillmentStatus,
        PaymentStatus paymentStatus,
        int itemCount,
        Map<String, BigDecimal> totalsByCurrency,
        OffsetDateTime createdAt
) {
    public static OrderListItemView from(Order order, String customerName) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            BigDecimal amount = item.getUnitPriceAmount();
            String currency = item.getUnitPriceCurrency();
            if (amount == null || currency == null) continue;
            BigDecimal line = amount.multiply(BigDecimal.valueOf(item.getQuantity()));
            totals.merge(currency, line, BigDecimal::add);
        }
        return new OrderListItemView(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customerName,
                order.getFulfillmentStatus(),
                order.getPaymentStatus(),
                order.getItems().size(),
                totals,
                order.getCreatedAt()
        );
    }
}
