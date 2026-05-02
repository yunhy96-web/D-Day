package com.hauly.platform.auth.presentation.dto;

/**
 * Optional request body for POST /api/auth/refresh.
 * Refresh token can also come from the hauly_rt cookie.
 */
public record RefreshRequest(String refreshToken) {}
