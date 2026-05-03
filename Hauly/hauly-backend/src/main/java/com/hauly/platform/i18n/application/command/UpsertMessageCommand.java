package com.hauly.platform.i18n.application.command;

/**
 * Command to upsert all 3 language rows for a message key.
 */
public record UpsertMessageCommand(
        String messageKey,
        String messageKo,
        String messageEn,
        String messageTh,
        String context
) {}
