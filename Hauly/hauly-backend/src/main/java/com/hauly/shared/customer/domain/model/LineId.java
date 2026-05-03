package com.hauly.shared.customer.domain.model;

/**
 * LINE messenger ID value object.
 * Trimmed, case-preserved (LINE IDs are case-sensitive). 1-64 chars per LINE's spec.
 */
public record LineId(String value) {

    private static final int MAX_LENGTH = 64;

    public LineId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LineId must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("LineId must be at most " + MAX_LENGTH + " characters");
        }
        value = normalized;
    }

    public static LineId of(String value) {
        return new LineId(value);
    }
}
