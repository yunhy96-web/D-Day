package com.hauly.shared.customer.domain.model;

/**
 * LINE messenger ID value object.
 * Trimmed, case-preserved. Length matches DB column (customer.line_id varchar(500)) —
 * users often paste full nickname/note here, so we don't enforce LINE's 1-64 spec.
 */
public record LineId(String value) {

    private static final int MAX_LENGTH = 500;

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
