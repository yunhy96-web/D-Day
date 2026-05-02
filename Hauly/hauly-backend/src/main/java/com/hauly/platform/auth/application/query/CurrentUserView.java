package com.hauly.platform.auth.application.query;

import com.hauly.platform.auth.domain.model.AppUser;

/**
 * Read model for the currently authenticated user.
 */
public record CurrentUserView(Long id, String email, String role, String displayName) {

    public static CurrentUserView from(AppUser user) {
        return new CurrentUserView(
                user.getId(),
                user.getEmailValue(),
                user.getRole().name(),
                user.getDisplayName()
        );
    }
}
