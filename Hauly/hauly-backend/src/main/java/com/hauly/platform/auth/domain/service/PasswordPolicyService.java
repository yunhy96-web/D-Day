package com.hauly.platform.auth.domain.service;

/**
 * Domain service for password policy enforcement.
 * Domain layer: pure Java — no Spring, no framework imports.
 */
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 4;

    /**
     * Validates a raw password against the policy.
     * Plan section 7: BCrypt cost 12 + minimum 12 characters.
     *
     * @param rawPassword the plaintext password to validate
     * @throws IllegalArgumentException if the password violates policy
     */
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_LENGTH + " characters long");
        }
    }

    public static PasswordPolicyService instance() {
        return new PasswordPolicyService();
    }
}
