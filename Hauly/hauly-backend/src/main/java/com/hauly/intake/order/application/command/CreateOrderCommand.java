package com.hauly.intake.order.application.command;

import com.hauly.intake.order.domain.model.OrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Command: register a new INTAKE order on behalf of a customer.
 * customerLineId / customerPhone are optional but at least one is recommended for matching.
 * orderType defaults to INDIVIDUAL when null.
 */
public record CreateOrderCommand(
        String customerName,
        String customerLineId,
        String customerPhone,
        OrderType orderType,
        String customerMemo,
        String internalMemo,
        String koreanTrackingNo,
        String koreanCourier,
        // Shipping address — all optional, applied to the aggregate after construction.
        String recipientName,
        String recipientPhone,
        String postalCode,
        String addressLine,
        String country,
        String shippingAddressLabel,
        List<Item> items
) {
    public record Item(
            String productName,
            String productUrl,
            Integer quantity,
            Long categoryId,
            Map<String, Object> attributes,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            List<String> tempImageKeys
    ) {}
}
