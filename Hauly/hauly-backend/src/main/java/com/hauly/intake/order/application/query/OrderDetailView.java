package com.hauly.intake.order.application.query;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderItem;
import com.hauly.intake.order.domain.model.OrderStatusLog;
import com.hauly.intake.order.domain.model.OrderType;
import com.hauly.intake.order.domain.model.PaymentStatus;
import com.hauly.intake.order.domain.model.StatusDimension;
import com.hauly.platform.storage.domain.BlobStorage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read model for the order detail page. Bundles allowed-next status sets so the UI
 * can render only the buttons that are valid from the current state.
 *
 * Image URLs are resolved at view-construction time via {@link BlobStorage#presignedGetUrl}
 * so the frontend can render &lt;img src&gt; directly without an extra round trip.
 */
public record OrderDetailView(
        Long id,
        String orderNo,
        Long customerId,
        String customerName,
        String customerLineId,
        String customerPhone,
        OrderType orderType,
        FulfillmentStatus fulfillmentStatus,
        PaymentStatus paymentStatus,
        Set<FulfillmentStatus> allowedFulfillmentNext,
        Set<PaymentStatus> allowedPaymentNext,
        String customerMemo,
        String internalMemo,
        String koreanTrackingNo,
        String koreanCourier,
        BigDecimal paidAmountKrw,
        List<String> purchaseProofKeys,
        List<String> purchaseProofUrls,
        String recipientName,
        String recipientPhone,
        String postalCode,
        String addressLine,
        String country,
        String shippingAddressLabel,
        // 재무
        BigDecimal customerRevenueAmount,
        String customerRevenueCurrency,
        BigDecimal logisticsKrToThAmount,
        String logisticsKrToThCurrency,
        BigDecimal logisticsThDomesticAmount,
        String logisticsThDomesticCurrency,
        BigDecimal krwPerThb,
        /** 계산된 순수익 (KRW). 입력 미완료 시 null. */
        BigDecimal netProfitKrw,
        List<Item> items,
        List<StatusLog> history,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** TTL for presigned URLs in the response — long enough to render the page comfortably. */
    private static final Duration IMAGE_URL_TTL = Duration.ofMinutes(15);

    public record Item(
            Long id,
            String productName,
            String productUrl,
            Integer quantity,
            Long categoryId,
            Map<String, Object> attributes,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            List<String> requestImageKeys,
            List<String> requestImageUrls
    ) {
        static Item from(OrderItem item, BlobStorage storage) {
            List<String> keys = item.getRequestImageKeys();
            List<String> urls = keys.stream()
                    .map(k -> storage.presignedGetUrl(k, IMAGE_URL_TTL))
                    .toList();
            return new Item(item.getId(), item.getProductName(), item.getProductUrl(),
                    item.getQuantity(), item.getCategoryId(), item.getAttributes(),
                    item.getUnitPriceAmount(), item.getUnitPriceCurrency(),
                    keys, urls);
        }
    }

    public record StatusLog(
            Long id,
            StatusDimension dimension,
            String fromCode,
            String toCode,
            Long changedBy,
            String note,
            boolean forced,
            OffsetDateTime createdAt
    ) {
        static StatusLog from(OrderStatusLog log) {
            return new StatusLog(log.getId(), log.getDimension(), log.getFromCode(),
                    log.getToCode(), log.getChangedBy(), log.getNote(), log.isForced(),
                    log.getCreatedAt());
        }
    }

    public static OrderDetailView from(Order order,
                                       String customerName,
                                       String customerLineId,
                                       String customerPhone,
                                       List<OrderStatusLog> history,
                                       BlobStorage storage) {
        return new OrderDetailView(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customerName,
                customerLineId,
                customerPhone,
                order.getOrderType(),
                order.getFulfillmentStatus(),
                order.getPaymentStatus(),
                order.getFulfillmentStatus().allowedNext(),
                order.getPaymentStatus().allowedNext(),
                order.getCustomerMemo(),
                order.getInternalMemo(),
                order.getKoreanTrackingNo(),
                order.getKoreanCourier(),
                order.getPaidAmountKrw(),
                order.getPurchaseProofKeys(),
                order.getPurchaseProofKeys().stream()
                        .map(k -> storage.presignedGetUrl(k, IMAGE_URL_TTL))
                        .toList(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getPostalCode(),
                order.getAddressLine(),
                order.getCountry(),
                order.getShippingAddressLabel(),
                order.getCustomerRevenueAmount(),
                order.getCustomerRevenueCurrency(),
                order.getLogisticsKrToThAmount(),
                order.getLogisticsKrToThCurrency(),
                order.getLogisticsThDomesticAmount(),
                order.getLogisticsThDomesticCurrency(),
                order.getKrwPerThb(),
                order.getNetProfitKrw(),
                order.getItems().stream().map(i -> Item.from(i, storage)).toList(),
                history.stream().map(StatusLog::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
