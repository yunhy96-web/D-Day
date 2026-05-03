package com.hauly.intake.order.application.query;

import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderItem;
import com.hauly.intake.order.domain.model.OrderStatusLog;
import com.hauly.intake.order.domain.model.PaymentStatus;
import com.hauly.intake.order.domain.model.StatusDimension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read model for the order detail page. Bundles allowed-next status sets so the UI
 * can render only the buttons that are valid from the current state.
 */
public record OrderDetailView(
        Long id,
        String orderNo,
        Long customerId,
        String customerName,
        String customerLineId,
        String customerPhone,
        FulfillmentStatus fulfillmentStatus,
        PaymentStatus paymentStatus,
        Set<FulfillmentStatus> allowedFulfillmentNext,
        Set<PaymentStatus> allowedPaymentNext,
        String customerMemo,
        String internalMemo,
        String koreanTrackingNo,
        String koreanCourier,
        List<Item> items,
        List<StatusLog> history,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record Item(
            Long id,
            String productName,
            String productUrl,
            Integer quantity,
            Long categoryId,
            Map<String, Object> attributes,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency
    ) {
        static Item from(OrderItem item) {
            return new Item(item.getId(), item.getProductName(), item.getProductUrl(),
                    item.getQuantity(), item.getCategoryId(), item.getAttributes(),
                    item.getUnitPriceAmount(), item.getUnitPriceCurrency());
        }
    }

    public record StatusLog(
            Long id,
            StatusDimension dimension,
            String fromCode,
            String toCode,
            Long changedBy,
            String note,
            OffsetDateTime createdAt
    ) {
        static StatusLog from(OrderStatusLog log) {
            return new StatusLog(log.getId(), log.getDimension(), log.getFromCode(),
                    log.getToCode(), log.getChangedBy(), log.getNote(), log.getCreatedAt());
        }
    }

    public static OrderDetailView from(Order order,
                                       String customerName,
                                       String customerLineId,
                                       String customerPhone,
                                       List<OrderStatusLog> history) {
        return new OrderDetailView(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customerName,
                customerLineId,
                customerPhone,
                order.getFulfillmentStatus(),
                order.getPaymentStatus(),
                order.getFulfillmentStatus().allowedNext(),
                order.getPaymentStatus().allowedNext(),
                order.getCustomerMemo(),
                order.getInternalMemo(),
                order.getKoreanTrackingNo(),
                order.getKoreanCourier(),
                order.getItems().stream().map(Item::from).toList(),
                history.stream().map(StatusLog::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
