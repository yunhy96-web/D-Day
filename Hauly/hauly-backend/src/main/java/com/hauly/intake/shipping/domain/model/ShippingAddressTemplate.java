package com.hauly.intake.shipping.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * 자주 쓰는 배송지를 별칭으로 저장하는 템플릿.
 * 운영자(selim/union/admin)가 공유하는 풀로 운영.
 */
@Entity
@Table(name = "shipping_address_template")
public class ShippingAddressTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String label;

    @Column(name = "recipient_name", length = 64)
    private String recipientName;

    @Column(name = "recipient_phone", length = 32)
    private String recipientPhone;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(length = 2)
    private String country;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ShippingAddressTemplate() {}

    public static ShippingAddressTemplate create(
            String label, String recipientName, String recipientPhone,
            String postalCode, String addressLine, String country, Long createdBy) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        ShippingAddressTemplate t = new ShippingAddressTemplate();
        t.label = label.trim();
        t.recipientName = blankToNull(recipientName);
        t.recipientPhone = blankToNull(recipientPhone);
        t.postalCode = blankToNull(postalCode);
        t.addressLine = blankToNull(addressLine);
        t.country = blankToNull(country);
        t.createdBy = createdBy;
        t.createdAt = OffsetDateTime.now();
        t.updatedAt = t.createdAt;
        return t;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getPostalCode() { return postalCode; }
    public String getAddressLine() { return addressLine; }
    public String getCountry() { return country; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
