package com.hauly.platform.category.application.command;

/**
 * Command to create a new product category.
 */
public record CreateCategoryCommand(
        String code,
        String nameKey,
        String fieldSchema,
        int sortOrder
) {}
