package com.hauly.platform.auth.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/auth/login.
 */
public record LoginRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 12, message = "Password must be at least 12 characters")
        String password
) {}
