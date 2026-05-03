package com.hauly.shared.customer.domain.model;

import java.util.regex.Pattern;

/**
 * E.164-normalised phone number value object.
 * Accepts inputs like "+66812345678", "010-1234-5678", "0812345678" and
 * stores the digits-only form prefixed with '+' if a country code is present.
 *
 * For Hauly MVP we are pragmatic: we accept any input that contains 8-15 digits.
 * Strict validation can come later when SMS verification is added.
 */
public record Phone(String value) {

    private static final Pattern DIGITS = Pattern.compile("\\d");
    private static final int MIN_DIGITS = 8;
    private static final int MAX_DIGITS = 15;

    public Phone {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone must not be blank");
        }
        String normalized = normalize(value);
        long digitCount = DIGITS.matcher(normalized).results().count();
        if (digitCount < MIN_DIGITS || digitCount > MAX_DIGITS) {
            throw new IllegalArgumentException(
                    "Phone must contain " + MIN_DIGITS + "-" + MAX_DIGITS + " digits: " + value);
        }
        value = normalized;
    }

    public static Phone of(String value) {
        return new Phone(value);
    }

    private static String normalize(String raw) {
        String trimmed = raw.trim();
        boolean hasPlus = trimmed.startsWith("+");
        StringBuilder digits = new StringBuilder();
        for (char c : trimmed.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        return hasPlus ? "+" + digits : digits.toString();
    }
}
