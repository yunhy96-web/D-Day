package com.hauly.platform.auth.domain.event;

import java.time.OffsetDateTime;

/**
 * Domain event published after a successful authentication.
 * Plain Java class — no Spring ApplicationEvent inheritance to keep domain pure.
 * Published via ApplicationEventPublisher in the application layer (AuthService).
 */
public record UserAuthenticatedEvent(
        Long userId,
        String username,
        String role,
        OffsetDateTime authenticatedAt
) {
    public static UserAuthenticatedEvent of(Long userId, String username, String role) {
        return new UserAuthenticatedEvent(userId, username, role, OffsetDateTime.now());
    }
}
