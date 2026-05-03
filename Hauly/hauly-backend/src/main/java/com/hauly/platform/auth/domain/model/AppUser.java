package com.hauly.platform.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.function.Function;

/**
 * AppUser aggregate root.
 * Pragmatic DDD: JPA annotations on the domain class (kept in domain/model/).
 * Domain logic lives here; infrastructure wires persistence.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(name = "display_name", length = 64)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Protected no-args constructor for JPA only. */
    protected AppUser() {}

    private AppUser(String username, String passwordHash, Role role, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Factory method — hashes raw password before creating the instance.
     */
    public static AppUser create(
            Username username,
            String rawPassword,
            Role role,
            String displayName,
            Function<String, String> passwordEncoder) {
        String hash = passwordEncoder.apply(rawPassword);
        return new AppUser(username.value(), hash, role, displayName);
    }

    /**
     * Verify raw password against stored hash.
     */
    public boolean verifyPassword(String rawPassword, java.util.function.BiFunction<String, String, Boolean> matcher) {
        return matcher.apply(rawPassword, this.passwordHash);
    }

    /** Replaces the stored hash with one freshly derived from {@code rawPassword}. */
    public void changePassword(String rawPassword, Function<String, String> passwordEncoder) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        this.passwordHash = passwordEncoder.apply(rawPassword);
        this.updatedAt = OffsetDateTime.now();
    }

    // --- Accessors (read-only from outside) ---

    public Long getId() { return id; }

    public Username getUsername() { return Username.of(username); }

    public String getUsernameValue() { return username; }

    String getPasswordHash() { return passwordHash; }

    public Role getRole() { return role; }

    public String getDisplayName() { return displayName; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
