package com.hauly.intake.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * OrderItem — child entity of {@link Order} aggregate.
 * MVP keeps it minimal: name, optional URL, quantity, optional notes.
 * Dynamic category attributes (lens diopter / BC / DIA) come in a later iteration.
 */
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "out_of_stock_note", columnDefinition = "TEXT")
    private String outOfStockNote;

    /**
     * Category-specific attributes — keyed by the field schema's `key` (e.g.
     * `brand`, `left_eye.power`). Stored as JSONB so we can vary the shape per category.
     * Domain validation against the schema lives in CategorySchemaValidator
     * (called from the application layer when the schema requires it).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    @Column(name = "unit_price_amount", precision = 12, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "unit_price_currency", length = 8)
    private String unitPriceCurrency;

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("KRW", "THB", "USD");

    /** JPA only. */
    protected OrderItem() {}

    OrderItem(Order order, String productName, String productUrl, Integer quantity,
              Long categoryId, Map<String, Object> attributes,
              BigDecimal unitPriceAmount, String unitPriceCurrency) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("product_name must not be blank");
        }
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        this.order = order;
        this.productName = productName.trim();
        this.productUrl = (productUrl == null || productUrl.isBlank()) ? null : productUrl.trim();
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.attributes = (attributes == null || attributes.isEmpty()) ? null : attributes;

        // Price/currency are paired: both present together or both absent.
        boolean hasAmount = unitPriceAmount != null;
        String currency = unitPriceCurrency == null ? null : unitPriceCurrency.trim().toUpperCase();
        boolean hasCurrency = currency != null && !currency.isEmpty();
        if (hasAmount != hasCurrency) {
            throw new IllegalArgumentException(
                    "unit_price_amount and unit_price_currency must be set together");
        }
        if (hasAmount && unitPriceAmount.signum() < 0) {
            throw new IllegalArgumentException("unit_price_amount must be >= 0");
        }
        if (hasCurrency && !ALLOWED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("unsupported currency: " + currency);
        }
        this.unitPriceAmount = hasAmount ? unitPriceAmount : null;
        this.unitPriceCurrency = hasCurrency ? currency : null;
    }

    void markOutOfStock(String note) {
        this.outOfStockNote = note;
    }

    // --- Accessors ---

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getProductName() { return productName; }
    public String getProductUrl() { return productUrl; }
    public Integer getQuantity() { return quantity; }
    public String getOutOfStockNote() { return outOfStockNote; }
    public Map<String, Object> getAttributes() { return attributes; }
    public BigDecimal getUnitPriceAmount() { return unitPriceAmount; }
    public String getUnitPriceCurrency() { return unitPriceCurrency; }
}
