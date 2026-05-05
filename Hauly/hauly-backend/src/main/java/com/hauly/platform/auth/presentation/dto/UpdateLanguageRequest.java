package com.hauly.platform.auth.presentation.dto;

import jakarta.validation.constraints.Pattern;

/**
 * PATCH /api/auth/me/language body. null/empty = clear preference (revert to global default).
 */
public record UpdateLanguageRequest(
        @Pattern(regexp = "^(ko|en|th)?$") String language
) {}
