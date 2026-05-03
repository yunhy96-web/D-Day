package com.hauly.platform.i18n.application;

import com.hauly.shared.kernel.Lang;
import com.hauly.platform.i18n.application.command.UpsertMessageCommand;
import com.hauly.platform.i18n.application.query.I18nMessageRowView;
import com.hauly.platform.i18n.domain.model.I18nMessage;
import com.hauly.platform.i18n.domain.model.I18nMessageId;
import com.hauly.platform.i18n.domain.repository.I18nMessageRepository;
import com.hauly.platform.support.exception.SystemProtectedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for i18n message operations.
 */
@Service
@Transactional
public class I18nMessageService {

    private final I18nMessageRepository messageRepository;

    public I18nMessageService(I18nMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "i18n.messages", key = "#lang.name()")
    public Map<String, String> getMessages(Lang lang) {
        String langCode = lang.name().toLowerCase();
        return messageRepository.findAllByLangCode(langCode).stream()
                .collect(Collectors.toMap(
                        I18nMessage::getMessageKey,
                        I18nMessage::getMessage,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "i18n.messages", key = "#lang.name() + '_' + #context")
    public Map<String, String> getMessages(Lang lang, String context) {
        String langCode = lang.name().toLowerCase();
        return messageRepository.findAllByLangCodeAndContext(langCode, context).stream()
                .collect(Collectors.toMap(
                        I18nMessage::getMessageKey,
                        I18nMessage::getMessage,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Admin: list all messages in row form (all langs per key).
     * Fetches all messages in one query, then groups in-memory to avoid N+1.
     */
    @Transactional(readOnly = true)
    public List<I18nMessageRowView> listAllRows(String context) {
        List<I18nMessage> allMessages = context != null
                ? messageRepository.findAllByContext(context)
                : messageRepository.findAll();

        // Group by messageKey, then by langCode
        Map<String, Map<String, I18nMessage>> grouped = allMessages.stream()
                .collect(Collectors.groupingBy(
                        I18nMessage::getMessageKey,
                        Collectors.toMap(I18nMessage::getLangCode, m -> m, (a, b) -> a)));

        return grouped.entrySet().stream()
                .map(e -> {
                    String key = e.getKey();
                    Map<String, I18nMessage> byLang = e.getValue();
                    I18nMessage koMsg = byLang.get("ko");
                    String ko = koMsg != null ? koMsg.getMessage() : null;
                    String en = byLang.containsKey("en") ? byLang.get("en").getMessage() : null;
                    String th = byLang.containsKey("th") ? byLang.get("th").getMessage() : null;
                    String ctx = koMsg != null ? koMsg.getContext() : (byLang.values().iterator().next().getContext());
                    boolean system = byLang.values().stream().anyMatch(I18nMessage::isSystem);
                    return new I18nMessageRowView(key, ko, en, th, ctx, system);
                })
                .sorted(java.util.Comparator.comparing(I18nMessageRowView::messageKey))
                .toList();
    }

    @CacheEvict(value = "i18n.messages", allEntries = true)
    public void upsertMessage(UpsertMessageCommand command) {
        upsertOne(command.messageKey(), "ko", command.messageKo(), command.context());
        upsertOne(command.messageKey(), "en", command.messageEn(), command.context());
        upsertOne(command.messageKey(), "th", command.messageTh(), command.context());
    }

    private void upsertOne(String key, String langCode, String text, String context) {
        if (text == null) return;
        I18nMessageId id = new I18nMessageId(key, langCode);
        messageRepository.findById(id).ifPresentOrElse(
                existing -> {
                    existing.updateMessage(text, context);
                    messageRepository.save(existing);
                },
                () -> messageRepository.save(I18nMessage.create(key, langCode, text, context))
        );
    }

    @CacheEvict(value = "i18n.messages", allEntries = true)
    public void deleteKey(String messageKey) {
        List<I18nMessage> rows = messageRepository.findAllByMessageKey(messageKey);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Message key not found: " + messageKey);
        }
        boolean isSystem = rows.stream().anyMatch(I18nMessage::isSystem);
        if (isSystem) {
            throw new SystemProtectedException("Cannot delete system message key: " + messageKey);
        }
        messageRepository.deleteAllByMessageKey(messageKey);
    }
}
