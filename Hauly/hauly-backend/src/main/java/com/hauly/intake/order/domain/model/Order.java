package com.hauly.intake.order.domain.model;

import com.hauly.intake.order.domain.exception.InvalidStatusTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Order aggregate root. Owns OrderItem children and emits OrderStatusLog entries.
 *
 * Two independent status dimensions:
 *  - {@link FulfillmentStatus}: REQUESTED → ACKNOWLEDGED → PURCHASING → … → COMPLETED
 *  - {@link PaymentStatus}:     NOT_REQUIRED (MVP default) | PENDING → SUBMITTED → CONFIRMED
 *
 * order_no scheme: HL-{yyyy}-{4-digit-id-suffix}. Assigned via {@link #assignOrderNo} after
 * the entity is persisted (because we need the DB-generated id).
 */
@Entity
@Table(name = "\"order\"")  // 'order' is a reserved word in SQL
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 20)
    private String orderNo;

    @Column(name = "customer_id")
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Origin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 16)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false, length = 32)
    private FulfillmentStatus fulfillmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;

    @Column(name = "customer_memo", columnDefinition = "TEXT")
    private String customerMemo;

    @Column(name = "internal_memo", columnDefinition = "TEXT")
    private String internalMemo;

    @Column(name = "korean_tracking_no", length = 64)
    private String koreanTrackingNo;

    @Column(name = "korean_courier", length = 32)
    private String koreanCourier;

    @Column(name = "recipient_name", length = 64)
    private String recipientName;

    @Column(name = "recipient_phone", length = 32)
    private String recipientPhone;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    /** ISO 3166-1 alpha-2 (TH/KR/US/...). */
    @Column(length = 2)
    private String country;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "purchased_by")
    private Long purchasedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Pending status-log writes — collected as the aggregate mutates and flushed by the
     * application service after save (so we have the persisted order id for log rows).
     */
    @Transient
    private final List<PendingLog> pendingLogs = new ArrayList<>();

    /** JPA only. */
    protected Order() {}

    private Order(Long customerId, Origin origin, OrderType orderType,
                  FulfillmentStatus fulfillmentStatus,
                  PaymentStatus paymentStatus, String customerMemo, String internalMemo,
                  Long createdBy) {
        this.customerId = customerId;
        this.origin = origin;
        this.orderType = orderType;
        this.fulfillmentStatus = fulfillmentStatus;
        this.paymentStatus = paymentStatus;
        this.customerMemo = customerMemo;
        this.internalMemo = internalMemo;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        // Placeholder — replaced by assignOrderNo once the DB id is available.
        this.orderNo = "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    /**
     * Factory: ADMIN_INTAKE order created by an INTAKE user (girlfriend).
     * Defaults: fulfillment_status=REQUESTED, payment_status=NOT_REQUIRED.
     */
    public static Order createIntake(Long customerId, OrderType orderType,
                                     String customerMemo, String internalMemo,
                                     String koreanTrackingNo, String koreanCourier,
                                     Long createdBy) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required for INTAKE order");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy (INTAKE user id) is required");
        }
        Order order = new Order(
                customerId,
                Origin.ADMIN_INTAKE,
                orderType == null ? OrderType.INDIVIDUAL : orderType,
                FulfillmentStatus.REQUESTED,
                PaymentStatus.NOT_REQUIRED,
                customerMemo,
                internalMemo,
                createdBy);
        order.koreanTrackingNo = blankToNull(koreanTrackingNo);
        order.koreanCourier = blankToNull(koreanCourier);
        order.pendingLogs.add(new PendingLog(
                StatusDimension.FULFILLMENT, null, FulfillmentStatus.REQUESTED.name(),
                createdBy, "Order registered"));
        return order;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * Sets the recipient delivery address. All fields optional and trimmed; blank → null.
     * Country is uppercased (ISO 3166-1 alpha-2 convention) for consistency.
     */
    public void setShippingAddress(String recipientName, String recipientPhone,
                                   String postalCode, String addressLine, String country) {
        this.recipientName = blankToNull(recipientName);
        this.recipientPhone = blankToNull(recipientPhone);
        this.postalCode = blankToNull(postalCode);
        this.addressLine = blankToNull(addressLine);
        String c = blankToNull(country);
        this.country = c == null ? null : c.toUpperCase();
        this.updatedAt = OffsetDateTime.now();
    }

    public OrderItem addItem(String productName, String productUrl, Integer quantity,
                             Long categoryId, Map<String, Object> attributes,
                             BigDecimal unitPriceAmount, String unitPriceCurrency) {
        OrderItem item = new OrderItem(this, productName, productUrl, quantity, categoryId,
                attributes, unitPriceAmount, unitPriceCurrency);
        items.add(item);
        return item;
    }

    /**
     * Assigned by the application service immediately after the first save() so that the
     * order id is available for formatting. Idempotent — accepts the new value once.
     */
    public void assignOrderNo(String generated) {
        if (generated == null || generated.isBlank()) {
            throw new IllegalArgumentException("orderNo must not be blank");
        }
        this.orderNo = generated;
        this.updatedAt = OffsetDateTime.now();
    }

    public void changeFulfillmentStatus(FulfillmentStatus target, Long changedBy, String note) {
        if (target == null) throw new IllegalArgumentException("target status required");
        if (!fulfillmentStatus.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition fulfillment_status from " + fulfillmentStatus + " to " + target);
        }
        FulfillmentStatus from = this.fulfillmentStatus;
        this.fulfillmentStatus = target;
        this.updatedAt = OffsetDateTime.now();
        pendingLogs.add(new PendingLog(
                StatusDimension.FULFILLMENT, from.name(), target.name(), changedBy, note));
    }

    public void changePaymentStatus(PaymentStatus target, Long changedBy, String note) {
        if (target == null) throw new IllegalArgumentException("target status required");
        if (!paymentStatus.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition payment_status from " + paymentStatus + " to " + target);
        }
        PaymentStatus from = this.paymentStatus;
        this.paymentStatus = target;
        this.updatedAt = OffsetDateTime.now();
        pendingLogs.add(new PendingLog(
                StatusDimension.PAYMENT, from.name(), target.name(), changedBy, note));
    }

    /** Drains the pending log list — called by the application service after save. */
    public List<PendingLog> drainPendingLogs() {
        List<PendingLog> snapshot = List.copyOf(pendingLogs);
        pendingLogs.clear();
        return snapshot;
    }

    // --- Accessors ---

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getCustomerId() { return customerId; }
    public Origin getOrigin() { return origin; }
    public OrderType getOrderType() { return orderType; }
    public FulfillmentStatus getFulfillmentStatus() { return fulfillmentStatus; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getCustomerMemo() { return customerMemo; }
    public String getInternalMemo() { return internalMemo; }
    public String getKoreanTrackingNo() { return koreanTrackingNo; }
    public String getKoreanCourier() { return koreanCourier; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getPostalCode() { return postalCode; }
    public String getAddressLine() { return addressLine; }
    public String getCountry() { return country; }
    public Long getCreatedBy() { return createdBy; }
    public Long getPurchasedBy() { return purchasedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    /** Carrier for a status change waiting to be written to order_status_log. */
    public record PendingLog(StatusDimension dimension, String fromCode, String toCode,
                             Long changedBy, String note) {}
}
