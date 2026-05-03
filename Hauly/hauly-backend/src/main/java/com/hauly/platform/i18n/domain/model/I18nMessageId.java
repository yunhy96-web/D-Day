package com.hauly.platform.i18n.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite PK for I18nMessage (message_key, lang_code).
 */
public class I18nMessageId implements Serializable {

    private String messageKey;
    private String langCode;

    public I18nMessageId() {}

    public I18nMessageId(String messageKey, String langCode) {
        this.messageKey = messageKey;
        this.langCode = langCode;
    }

    public String getMessageKey() { return messageKey; }
    public String getLangCode() { return langCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof I18nMessageId that)) return false;
        return Objects.equals(messageKey, that.messageKey) && Objects.equals(langCode, that.langCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageKey, langCode);
    }
}
