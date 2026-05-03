package com.hauly.platform.i18n.domain.repository;

import com.hauly.platform.i18n.domain.model.I18nMessage;
import com.hauly.platform.i18n.domain.model.I18nMessageId;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for I18nMessage.
 * Plain Java — no Spring/JPA imports.
 */
public interface I18nMessageRepository {

    List<I18nMessage> findAll();

    List<I18nMessage> findAllByContext(String context);

    List<I18nMessage> findAllByLangCode(String langCode);

    List<I18nMessage> findAllByLangCodeAndContext(String langCode, String context);

    Optional<I18nMessage> findById(I18nMessageId id);

    List<I18nMessage> findAllByMessageKey(String messageKey);

    I18nMessage save(I18nMessage message);

    void deleteAllByMessageKey(String messageKey);
}
