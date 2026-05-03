package com.hauly.platform.category.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String code,
        @NotBlank String nameKey,
        String fieldSchema,
        int sortOrder
) {}
