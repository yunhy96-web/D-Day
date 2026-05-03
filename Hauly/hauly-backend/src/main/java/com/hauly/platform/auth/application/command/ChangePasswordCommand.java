package com.hauly.platform.auth.application.command;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {}
