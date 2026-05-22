package com.flasharena.auth.presentation.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UUID userId,
        String role,
        long expiresIn) {
}
