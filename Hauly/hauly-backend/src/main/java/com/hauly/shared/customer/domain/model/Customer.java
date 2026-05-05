package com.hauly.shared.customer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Customer aggregate root.
 * Pragmatic DDD: JPA on the domain class.
 *
 * MVP scope: created as GUEST during INTAKE. line_id / phone serve as natural keys
 * for auto-matching (re-use existing customer instead of creating duplicates).
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 16)
    private AccountStatus accountStatus;

    @Column(name = "line_id", length = 500)
    private String lineId;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "default_address")
    private String defaultAddress;

    @Column(name = "preferred_lang", length = 8)
    private String preferredLang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** JPA only. */
    protected Customer() {}

    private Customer(AccountStatus accountStatus, String name, String lineId, String phone) {
        this.accountStatus = accountStatus;
        this.name = name;
        this.lineId = lineId;
        this.phone = phone;
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Factory: create a GUEST customer (used by INTAKE flow).
     * Either line_id or phone (or both) should be provided so future orders can be matched.
     */
    public static Customer createGuest(String name, LineId lineId, Phone phone) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        return new Customer(
                AccountStatus.GUEST,
                name.trim(),
                lineId == null ? null : lineId.value(),
                phone == null ? null : phone.value()
        );
    }

    // --- Mutators (intentionally narrow) ---

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        this.name = newName.trim();
    }

    // --- Accessors ---

    public Long getId() { return id; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public String getLineId() { return lineId; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getDefaultAddress() { return defaultAddress; }
    public String getPreferredLang() { return preferredLang; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
