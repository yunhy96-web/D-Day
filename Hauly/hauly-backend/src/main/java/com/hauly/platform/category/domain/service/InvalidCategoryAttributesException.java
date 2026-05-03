package com.hauly.platform.category.domain.service;

import com.hauly.platform.category.domain.model.ValidationResult;
import com.hauly.platform.support.exception.HaulyException;

import java.util.List;

/**
 * Thrown when category attribute validation fails.
 */
public class InvalidCategoryAttributesException extends HaulyException {

    private final List<ValidationResult.FieldError> fieldErrors;

    public InvalidCategoryAttributesException(List<ValidationResult.FieldError> fieldErrors) {
        super("INVALID_CATEGORY_ATTRIBUTES", "Category attributes validation failed");
        this.fieldErrors = fieldErrors;
    }

    public List<ValidationResult.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
