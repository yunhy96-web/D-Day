package com.hauly.platform.category.application.query;

import com.hauly.platform.category.domain.model.FieldDefinition;
import com.hauly.platform.category.domain.model.ProductCategory;

import java.util.List;

/**
 * Detailed view including parsed schema.
 */
public record CategoryDetailView(
        Long id,
        String code,
        String nameKey,
        int sortOrder,
        boolean active,
        List<FieldDefinition> fields
) {
    public static CategoryDetailView from(ProductCategory c, List<FieldDefinition> fields) {
        return new CategoryDetailView(
                c.getId(), c.getCode(), c.getNameKey(), c.getSortOrder(), c.isActive(), fields);
    }
}
