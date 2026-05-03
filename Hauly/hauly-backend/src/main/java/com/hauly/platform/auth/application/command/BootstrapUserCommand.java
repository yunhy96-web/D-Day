package com.hauly.platform.auth.application.command;

import com.hauly.platform.auth.domain.model.Role;

/**
 * Command for bootstrapping an initial user via CommandLineRunner.
 */
public record BootstrapUserCommand(String username, String rawPassword, Role role, String displayName) {}
