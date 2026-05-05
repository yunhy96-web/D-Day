package com.hauly.intake.order.application.query;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderItem;
import com.hauly.intake.order.domain.model.OrderType;
import com.hauly.intake.order.domain.model.PaymentStatus;
import com.hauly.platform.storage.domain.BlobStorage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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
        OrderType orderType,
        FulfillmentStatus fulfillmentStatus,
        PaymentStatus paymentStatus,
        int itemCount,
        // 첫 번째 상품의 product_name — 목록에서 빠르게 무엇을 주문했는지 식별.
        String firstProductName,
        // 첫 번째 상품의 첫 번째 이미지 (presigned). 없으면 null.
        String firstImageUrl,
        String koreanCourier,
        String koreanTrackingNo,
        String shippingAddressLabel,
        // PURCHASED 시 입력한 실제 결제 금액 (KRW). 미입력 또는 PURCHASED 미진입 시 null.
        BigDecimal paidAmountKrw,
        Map<String, BigDecimal> totalsByCurrency,
        OffsetDateTime createdAt
) {
    /** TTL for presigned URLs in the list — short list paint window. */
    private static final Duration IMAGE_URL_TTL = Duration.ofMinutes(15);

    public static OrderListItemView from(Order order, String customerName, BlobStorage storage) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            BigDecimal amount = item.getUnitPriceAmount();
            String currency = item.getUnitPriceCurrency();
            if (amount == null || currency == null) continue;
            BigDecimal line = amount.multiply(BigDecimal.valueOf(item.getQuantity()));
            totals.merge(currency, line, BigDecimal::add);
        }
        String firstName = null;
        String firstImageUrl = null;
        if (!order.getItems().isEmpty()) {
            OrderItem first = order.getItems().get(0);
            firstName = first.getProductName();
            List<String> keys = first.getRequestImageKeys();
            if (keys != null && !keys.isEmpty()) {
                firstImageUrl = storage.presignedGetUrl(keys.get(0), IMAGE_URL_TTL);
            }
        }
        return new OrderListItemView(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customerName,
                order.getOrderType(),
                order.getFulfillmentStatus(),
                order.getPaymentStatus(),
                order.getItems().size(),
                firstName,
                firstImageUrl,
                order.getKoreanCourier(),
                order.getKoreanTrackingNo(),
                order.getShippingAddressLabel(),
                order.getPaidAmountKrw(),
                totals,
                order.getCreatedAt()
        );
    }
}
