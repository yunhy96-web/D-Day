package com.flasharena.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username 은 필수입니다.") String username,
        @NotBlank(message = "password 는 필수입니다.") String password) {
}
