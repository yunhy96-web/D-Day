package com.hauly.platform.category.domain.model;

import java.util.List;

/**
 * Parsed representation of a single field in a category schema.
 * Domain layer — no Spring/JPA imports.
 */
public record FieldDefinition(
        String key,
        String labelKey,
        String type,          // text | decimal | select | group
        boolean required,
        String optionsCode,   // for type=select: common code group code
        Double min,
        Double max,
        Double step,
        List<FieldDefinition> fields  // for type=group
) {}
