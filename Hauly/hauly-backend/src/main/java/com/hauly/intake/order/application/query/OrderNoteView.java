package com.hauly.intake.order.application.query;

import com.hauly.intake.order.domain.model.OrderNote;

import java.time.OffsetDateTime;

/**
 * Read model for a single note. Includes the author display name resolved from app_user
 * so the frontend can render "{author}: ..." without an extra lookup.
 */
public record OrderNoteView(
        Long id,
        Long authorId,
        String authorName,
        String body,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean edited
) {
    public static OrderNoteView from(OrderNote note, String authorName) {
        return new OrderNoteView(
                note.getId(),
                note.getAuthorId(),
                authorName,
                note.getBody(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.wasEdited()
        );
    }
}
