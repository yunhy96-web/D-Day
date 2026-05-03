package com.hauly.platform.i18n.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * I18nMessage entity with composite PK (message_key, lang_code).
 * Pragmatic DDD: JPA annotations on domain class.
 */
@Entity
@Table(name = "i18n_message")
@IdClass(I18nMessageId.class)
public class I18nMessage {

    @Id
    @Column(name = "message_key", length = 128)
    private String messageKey;

    @Id
    @Column(name = "lang_code", length = 8)
    private String langCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 64)
    private String context;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected I18nMessage() {}

    private I18nMessage(String messageKey, String langCode, String message, String context, boolean system) {
        this.messageKey = messageKey;
        this.langCode = langCode;
        this.message = message;
        this.context = context;
        this.system = system;
        this.updatedAt = OffsetDateTime.now();
    }

    public static I18nMessage create(String messageKey, String langCode, String message, String context) {
        if (messageKey == null || messageKey.isBlank()) throw new IllegalArgumentException("messageKey required");
        if (langCode == null || langCode.isBlank()) throw new IllegalArgumentException("langCode required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message required");
        return new I18nMessage(messageKey, langCode, message, context, false);
    }

    public void updateMessage(String message, String context) {
        this.message = message;
        this.context = context;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getMessageKey() { return messageKey; }
    public String getLangCode() { return langCode; }
    public String getMessage() { return message; }
    public String getContext() { return context; }
    public boolean isSystem() { return system; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
