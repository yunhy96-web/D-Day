package com.hauly.platform.category.domain.model;

import java.util.List;

/**
 * Result of schema attribute validation.
 */
public record ValidationResult(boolean isValid, List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(List<FieldError> errors) {
        return new ValidationResult(false, errors);
    }
}
