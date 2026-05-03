package com.hauly.intake.order.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Command: register a new INTAKE order on behalf of a customer.
 * customerLineId / customerPhone are optional but at least one is recommended for matching.
 */
public record CreateOrderCommand(
        String customerName,
        String customerLineId,
        String customerPhone,
        String customerMemo,
        String internalMemo,
        String koreanTrackingNo,
        String koreanCourier,
        List<Item> items
) {
    public record Item(
            String productName,
            String productUrl,
            Integer quantity,
            Long categoryId,
            Map<String, Object> attributes,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency
    ) {}
}
