package com.hauly.platform.auth.domain.model;

import java.util.Set;

/**
 * Role value object — maps to common_code USER_ROLE group.
 * Domain layer: no Spring, no JPA imports.
 */
public enum Role {
    INTAKE,
    BUYER,
    ADMIN,
    VIEWER;

    /**
     * Returns Spring Security-style authority strings for this role.
     */
    public Set<String> authorities() {
        return Set.of("ROLE_" + this.name());
    }

    public static Role fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role value must not be null");
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }
}
