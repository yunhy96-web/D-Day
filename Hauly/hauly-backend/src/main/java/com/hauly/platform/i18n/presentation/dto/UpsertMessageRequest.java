package com.hauly.platform.i18n.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertMessageRequest(
        @NotBlank String ko,
        String en,
        String th,
        String context
) {}
