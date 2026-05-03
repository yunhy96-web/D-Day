package com.hauly.platform.auth.application.command;

/**
 * Command for user login use case.
 */
public record LoginCommand(String username, String password) {}
