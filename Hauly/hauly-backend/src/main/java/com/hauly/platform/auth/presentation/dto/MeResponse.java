package com.hauly.platform.auth.presentation.dto;

import com.hauly.platform.auth.application.query.CurrentUserView;

/**
 * Response body for GET /api/auth/me.
 */
public record MeResponse(Long id, String email, String role, String displayName) {

    public static MeResponse from(CurrentUserView view) {
        return new MeResponse(view.id(), view.email(), view.role(), view.displayName());
    }
}
