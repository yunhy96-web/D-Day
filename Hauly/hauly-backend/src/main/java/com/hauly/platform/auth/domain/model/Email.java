package com.hauly.platform.auth.domain.model;

import java.util.regex.Pattern;

/**
 * Email value object.
 * Domain layer: no Spring, no JPA, no HTTP imports.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final int MAX_LENGTH = 128;

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email must be at most " + MAX_LENGTH + " characters");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + normalized);
        }
        value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
