package com.hauly.platform.i18n.infrastructure.persistence;

import com.hauly.platform.i18n.domain.model.I18nMessage;
import com.hauly.platform.i18n.domain.model.I18nMessageId;
import com.hauly.platform.i18n.domain.repository.I18nMessageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA implementation of I18nMessageRepository.
 */
@Repository
public interface JpaI18nMessageRepository
        extends JpaRepository<I18nMessage, I18nMessageId>, I18nMessageRepository {

    @Query("SELECT m FROM I18nMessage m ORDER BY m.messageKey, m.langCode")
    List<I18nMessage> findAll();

    @Query("SELECT m FROM I18nMessage m WHERE m.context = :context ORDER BY m.messageKey, m.langCode")
    List<I18nMessage> findAllByContext(@Param("context") String context);

    @Query("SELECT m FROM I18nMessage m WHERE m.langCode = :langCode ORDER BY m.messageKey")
    List<I18nMessage> findAllByLangCode(@Param("langCode") String langCode);

    @Query("SELECT m FROM I18nMessage m WHERE m.langCode = :langCode AND m.context = :context ORDER BY m.messageKey")
    List<I18nMessage> findAllByLangCodeAndContext(@Param("langCode") String langCode,
                                                  @Param("context") String context);

    @Query("SELECT m FROM I18nMessage m WHERE m.messageKey = :messageKey")
    List<I18nMessage> findAllByMessageKey(@Param("messageKey") String messageKey);

    @Modifying
    @Query("DELETE FROM I18nMessage m WHERE m.messageKey = :messageKey")
    void deleteAllByMessageKey(@Param("messageKey") String messageKey);
}
