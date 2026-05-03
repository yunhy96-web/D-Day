package com.hauly.platform.i18n;

import com.hauly.shared.kernel.Lang;
import com.hauly.platform.support.exception.SystemProtectedException;
import com.hauly.platform.i18n.application.I18nMessageService;
import com.hauly.platform.i18n.application.command.UpsertMessageCommand;
import com.hauly.platform.i18n.domain.model.I18nMessage;
import com.hauly.platform.i18n.domain.model.I18nMessageId;
import com.hauly.platform.i18n.domain.repository.I18nMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class I18nMessageServiceTest {

    @Mock
    private I18nMessageRepository messageRepository;

    @InjectMocks
    private I18nMessageService service;

    @Test
    void getMessages_lang_returnsMapOfMessages() {
        I18nMessage msg = I18nMessage.create("btn.cancel", "ko", "취소", "common");
        when(messageRepository.findAllByLangCode("ko")).thenReturn(List.of(msg));

        Map<String, String> result = service.getMessages(Lang.KO);

        assertThat(result).containsEntry("btn.cancel", "취소");
    }

    @Test
    void getMessages_langAndContext_returnsFilteredMessages() {
        I18nMessage msg = I18nMessage.create("btn.cancel", "ko", "취소", "common");
        when(messageRepository.findAllByLangCodeAndContext("ko", "common")).thenReturn(List.of(msg));

        Map<String, String> result = service.getMessages(Lang.KO, "common");

        assertThat(result).containsEntry("btn.cancel", "취소");
        assertThat(result).hasSize(1);
    }

    @Test
    void upsertMessage_newKey_savesAll3Langs() {
        when(messageRepository.findById(any(I18nMessageId.class))).thenReturn(Optional.empty());
        when(messageRepository.save(any(I18nMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        UpsertMessageCommand cmd = new UpsertMessageCommand("btn.new", "새 버튼", "New Button", "ปุ่มใหม่", "common");
        service.upsertMessage(cmd);

        verify(messageRepository, times(3)).save(any(I18nMessage.class));
    }

    @Test
    void upsertMessage_existingKey_updatesInsteadOfCreating() {
        I18nMessage existing = I18nMessage.create("btn.cancel", "ko", "취소", "common");
        when(messageRepository.findById(new I18nMessageId("btn.cancel", "ko")))
                .thenReturn(Optional.of(existing));
        when(messageRepository.findById(new I18nMessageId("btn.cancel", "en")))
                .thenReturn(Optional.empty());
        when(messageRepository.findById(new I18nMessageId("btn.cancel", "th")))
                .thenReturn(Optional.empty());
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpsertMessageCommand cmd = new UpsertMessageCommand("btn.cancel", "취소2", "Cancel2", "ยกเลิก2", "common");
        service.upsertMessage(cmd);

        // saves for en and th (new), and updates for ko
        verify(messageRepository, times(3)).save(any(I18nMessage.class));
        assertThat(existing.getMessage()).isEqualTo("취소2");
    }

    @Test
    void deleteKey_systemMessage_throwsException() {
        I18nMessage systemMsg = org.mockito.Mockito.mock(I18nMessage.class);
        when(systemMsg.isSystem()).thenReturn(true);
        when(messageRepository.findAllByMessageKey("btn.cancel")).thenReturn(List.of(systemMsg));

        assertThatThrownBy(() -> service.deleteKey("btn.cancel"))
                .isInstanceOf(SystemProtectedException.class);
    }

    @Test
    void deleteKey_nonSystemMessage_deletesAllLangs() {
        I18nMessage msg = I18nMessage.create("custom.key", "ko", "커스텀", "common");
        when(messageRepository.findAllByMessageKey("custom.key")).thenReturn(List.of(msg));

        service.deleteKey("custom.key");

        verify(messageRepository).deleteAllByMessageKey("custom.key");
    }
}
