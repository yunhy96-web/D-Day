package com.hauly.platform.auth.application.command;

/**
 * Command for token refresh use case.
 */
public record RefreshCommand(String refreshToken) {}
