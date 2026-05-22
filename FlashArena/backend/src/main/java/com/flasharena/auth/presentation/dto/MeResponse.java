package com.flasharena.auth.presentation.dto;

import java.util.UUID;

public record MeResponse(UUID userId, String role) {
}
