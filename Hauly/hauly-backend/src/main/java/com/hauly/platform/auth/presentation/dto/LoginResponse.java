package com.hauly.platform.auth.presentation.dto;

import com.hauly.platform.auth.application.query.CurrentUserView;

/**
 * Response body for POST /api/auth/login.
 */
public record LoginResponse(String accessToken, String refreshToken, CurrentUserView user) {}
