package com.hauly.platform.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/auth/login.
 */
public record LoginRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 32, message = "Username must be 3-32 characters")
        String username,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 4, message = "Password must be at least 4 characters")
        String password
) {}
