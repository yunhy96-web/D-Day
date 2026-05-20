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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "korean_tracking_no", length = 500)
    private String koreanTrackingNo;

    @Column(name = "korean_courier", length = 32)
    private String koreanCourier;

    /** PURCHASED 시점에 기록되는 실제 결제 금액 (KRW). NULL 가능. */
    @Column(name = "paid_amount_krw", precision = 15, scale = 2)
    private java.math.BigDecimal paidAmountKrw;

    /** PURCHASED 후 캡처한 결제 증빙 이미지 키 목록. NULL 가능. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purchase_proof_keys", columnDefinition = "jsonb")
    private List<String> purchaseProofKeys;

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

    /** 배송지 템플릿 별칭 (선택). 템플릿이 삭제돼도 라벨 텍스트는 남도록 비정규화. */
    @Column(name = "shipping_address_label", length = 64)
    private String shippingAddressLabel;

    // --- 재무 필드 (순수익 계산용. 모두 nullable, 단계적 입력 허용.) ---

    /** 고객이 송금한 금액 (KRW or THB). */
    @Column(name = "customer_revenue_amount", precision = 12, scale = 2)
    private java.math.BigDecimal customerRevenueAmount;

    @Column(name = "customer_revenue_currency", length = 3)
    private String customerRevenueCurrency;

    /** 한→태 국제 물류비. */
    @Column(name = "logistics_kr_to_th_amount", precision = 12, scale = 2)
    private java.math.BigDecimal logisticsKrToThAmount;

    @Column(name = "logistics_kr_to_th_currency", length = 3)
    private String logisticsKrToThCurrency;

    /** 태국 내 배송비. */
    @Column(name = "logistics_th_domestic_amount", precision = 12, scale = 2)
    private java.math.BigDecimal logisticsThDomesticAmount;

    @Column(name = "logistics_th_domestic_currency", length = 3)
    private String logisticsThDomesticCurrency;

    /** 주문별 환율 (1 THB = N KRW). THB 값 환산용. */
    @Column(name = "krw_per_thb", precision = 10, scale = 4)
    private java.math.BigDecimal krwPerThb;

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
                createdBy, "Order registered", false));
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

    public void setShippingAddressLabel(String label) {
        this.shippingAddressLabel = blankToNull(label);
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
                StatusDimension.FULFILLMENT, from.name(), target.name(), changedBy, note, false));
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
                StatusDimension.PAYMENT, from.name(), target.name(), changedBy, note, false));
    }

    /**
     * ADMIN-only override: jump to any other fulfillment status, bypassing the state machine.
     * Used to reverse mistakes (e.g. roll back from PURCHASED when the upstream brand cancels).
     * The reason is stored in the audit log and is required upstream.
     */
    public void forceChangeFulfillmentStatus(FulfillmentStatus target, Long changedBy, String reason) {
        if (target == null) throw new IllegalArgumentException("target status required");
        if (target == fulfillmentStatus) {
            throw new IllegalArgumentException("force_same_status");
        }
        FulfillmentStatus from = this.fulfillmentStatus;
        this.fulfillmentStatus = target;
        this.updatedAt = OffsetDateTime.now();
        pendingLogs.add(new PendingLog(
                StatusDimension.FULFILLMENT, from.name(), target.name(), changedBy, reason, true));
    }

    /** ADMIN-only override mirror of {@link #forceChangeFulfillmentStatus} for the payment dimension. */
    public void forceChangePaymentStatus(PaymentStatus target, Long changedBy, String reason) {
        if (target == null) throw new IllegalArgumentException("target status required");
        if (target == paymentStatus) {
            throw new IllegalArgumentException("force_same_status");
        }
        PaymentStatus from = this.paymentStatus;
        this.paymentStatus = target;
        this.updatedAt = OffsetDateTime.now();
        pendingLogs.add(new PendingLog(
                StatusDimension.PAYMENT, from.name(), target.name(), changedBy, reason, true));
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
    public java.math.BigDecimal getPaidAmountKrw() { return paidAmountKrw; }
    public void recordPaidAmountKrw(java.math.BigDecimal amount) { this.paidAmountKrw = amount; }
    public List<String> getPurchaseProofKeys() {
        return purchaseProofKeys == null ? List.of() : Collections.unmodifiableList(purchaseProofKeys);
    }
    public void recordPurchaseProofKeys(List<String> keys) {
        this.purchaseProofKeys = (keys == null || keys.isEmpty()) ? null : new ArrayList<>(keys);
    }
    /** 트래킹 정보 갱신 (PURCHASED 이후에도 호출 가능). 빈 문자열은 null로 정규화. */
    public void updateTracking(String courier, String trackingNo) {
        this.koreanCourier = blankToNull(courier);
        this.koreanTrackingNo = blankToNull(trackingNo);
        this.updatedAt = OffsetDateTime.now();
    }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getPostalCode() { return postalCode; }
    public String getAddressLine() { return addressLine; }
    public String getCountry() { return country; }
    public String getShippingAddressLabel() { return shippingAddressLabel; }
    public Long getCreatedBy() { return createdBy; }
    public Long getPurchasedBy() { return purchasedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    public java.math.BigDecimal getCustomerRevenueAmount() { return customerRevenueAmount; }
    public String getCustomerRevenueCurrency() { return customerRevenueCurrency; }
    public java.math.BigDecimal getLogisticsKrToThAmount() { return logisticsKrToThAmount; }
    public String getLogisticsKrToThCurrency() { return logisticsKrToThCurrency; }
    public java.math.BigDecimal getLogisticsThDomesticAmount() { return logisticsThDomesticAmount; }
    public String getLogisticsThDomesticCurrency() { return logisticsThDomesticCurrency; }
    public java.math.BigDecimal getKrwPerThb() { return krwPerThb; }

    /**
     * 재무 필드 일괄 업데이트. null/빈값은 해당 필드 클리어.
     * amount-currency 쌍은 함께 또는 둘 다 비어있어야 함 (정합성).
     */
    public void updateFinancials(
            java.math.BigDecimal customerRevenueAmount, String customerRevenueCurrency,
            java.math.BigDecimal logisticsKrToThAmount, String logisticsKrToThCurrency,
            java.math.BigDecimal logisticsThDomesticAmount, String logisticsThDomesticCurrency,
            java.math.BigDecimal krwPerThb) {
        validatePair(customerRevenueAmount, customerRevenueCurrency, "customer_revenue");
        validatePair(logisticsKrToThAmount, logisticsKrToThCurrency, "logistics_kr_to_th");
        validatePair(logisticsThDomesticAmount, logisticsThDomesticCurrency, "logistics_th_domestic");
        this.customerRevenueAmount = customerRevenueAmount;
        this.customerRevenueCurrency = blankToNull(customerRevenueCurrency);
        this.logisticsKrToThAmount = logisticsKrToThAmount;
        this.logisticsKrToThCurrency = blankToNull(logisticsKrToThCurrency);
        this.logisticsThDomesticAmount = logisticsThDomesticAmount;
        this.logisticsThDomesticCurrency = blankToNull(logisticsThDomesticCurrency);
        this.krwPerThb = krwPerThb;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validatePair(java.math.BigDecimal amount, String currency, String fieldName) {
        boolean hasAmount = amount != null;
        boolean hasCurrency = currency != null && !currency.isBlank();
        if (hasAmount != hasCurrency) {
            throw new IllegalArgumentException(fieldName + "_amount_currency_mismatch");
        }
        if (hasCurrency && !"KRW".equals(currency) && !"THB".equals(currency)) {
            throw new IllegalArgumentException(fieldName + "_invalid_currency");
        }
    }

    /**
     * 순수익 (KRW) 계산. 4개 monetary 입력값이 모두 채워져 있고, THB값이 있을 때 환율도 입력되어
     * 있어야 계산 가능. 그렇지 않으면 null 반환 (UI가 적절한 메시지로 fallback).
     *
     *   profit_krw = customer_revenue_krw - paid_amount_krw - logistics_kr_th_krw - logistics_th_dom_krw
     */
    public java.math.BigDecimal getNetProfitKrw() {
        // 핵심 두 입력만 필수: 실결제금액 + 매출. 물류비는 미입력 시 0으로 간주.
        if (paidAmountKrw == null || customerRevenueAmount == null) {
            return null;
        }
        java.math.BigDecimal revenueKrw = toKrw(customerRevenueAmount, customerRevenueCurrency);
        java.math.BigDecimal logisticsKr = logisticsKrToThAmount == null
                ? java.math.BigDecimal.ZERO
                : toKrw(logisticsKrToThAmount, logisticsKrToThCurrency);
        java.math.BigDecimal logisticsTh = logisticsThDomesticAmount == null
                ? java.math.BigDecimal.ZERO
                : toKrw(logisticsThDomesticAmount, logisticsThDomesticCurrency);
        // THB 입력값이 있는데 환율이 없으면 변환 불가 → null
        if (revenueKrw == null || logisticsKr == null || logisticsTh == null) return null;
        return revenueKrw.subtract(paidAmountKrw).subtract(logisticsKr).subtract(logisticsTh);
    }

    /** 순수익 (THB) — 환율(krwPerThb) 입력된 주문에서만 계산 가능. 없으면 null. */
    public java.math.BigDecimal getNetProfitThb() {
        java.math.BigDecimal profitKrw = getNetProfitKrw();
        if (profitKrw == null || krwPerThb == null || krwPerThb.signum() == 0) return null;
        return profitKrw.divide(krwPerThb, 2, java.math.RoundingMode.HALF_UP);
    }

    private java.math.BigDecimal toKrw(java.math.BigDecimal amount, String currency) {
        if (amount == null || currency == null) return null;
        if ("KRW".equals(currency)) return amount;
        if ("THB".equals(currency)) {
            if (krwPerThb == null) return null;
            return amount.multiply(krwPerThb).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    /** PURCHASED 이후 결제 금액 직접 수정 (디파짓 보정은 service 레이어에서 처리). */
    public void overridePaidAmountKrw(java.math.BigDecimal newAmount) {
        if (newAmount == null || newAmount.signum() <= 0) {
            throw new IllegalArgumentException("paid_amount_required");
        }
        this.paidAmountKrw = newAmount;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Carrier for a status change waiting to be written to order_status_log. */
    public record PendingLog(StatusDimension dimension, String fromCode, String toCode,
                             Long changedBy, String note, boolean forced) {}
}
