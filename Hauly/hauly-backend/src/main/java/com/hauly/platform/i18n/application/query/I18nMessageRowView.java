package com.hauly.platform.i18n.application.query;

/**
 * Admin read model — one row per key with all 3 language values.
 */
public record I18nMessageRowView(
        String messageKey,
        String messageKo,
        String messageEn,
        String messageTh,
        String context,
        boolean system
) {}
