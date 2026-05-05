package com.hauly.platform.auth.application.query;

import com.hauly.platform.auth.domain.model.AppUser;

/**
 * Read model for the currently authenticated user.
 */
public record CurrentUserView(Long id, String username, String role, String displayName,
                              String preferredLanguage) {

    public static CurrentUserView from(AppUser user) {
        return new CurrentUserView(
                user.getId(),
                user.getUsernameValue(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getPreferredLanguage()
        );
    }
}
