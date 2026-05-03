package com.hauly.platform.auth.domain.model;

import java.util.regex.Pattern;

/**
 * Username value object — short login id (e.g. "selim", "union").
 * Replaces the previous Email VO; we never used email as a notification channel
 * and short ids are easier for the two operators to type.
 *
 * Domain layer: no Spring, no JPA, no HTTP imports.
 */
public record Username(String value) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        // Lower-case so logins are case-insensitive — operators won't remember caps.
        String normalized = value.trim().toLowerCase();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid username format (3-32 chars, letters/digits/_/-): " + normalized);
        }
        value = normalized;
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
