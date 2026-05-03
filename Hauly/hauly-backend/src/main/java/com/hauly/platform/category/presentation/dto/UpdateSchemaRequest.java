package com.hauly.platform.category.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSchemaRequest(@NotBlank String fieldSchema) {}
