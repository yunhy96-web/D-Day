package com.hauly.platform.category.application.query;

import com.hauly.platform.category.domain.model.FieldDefinition;
import com.hauly.platform.category.domain.model.ProductCategory;

import java.util.List;

/**
 * Summary view for category list — includes parsed field schema so the admin SPA
 * can render dynamic inputs without a second round-trip per category.
 */
public record CategoryView(
        Long id,
        String code,
        String nameKey,
        int sortOrder,
        boolean active,
        List<FieldDefinition> fields
) {
    public static CategoryView from(ProductCategory c, List<FieldDefinition> fields) {
        return new CategoryView(
                c.getId(), c.getCode(), c.getNameKey(),
                c.getSortOrder(), c.isActive(), fields);
    }
}
