package com.hauly.platform.commoncode;

import com.hauly.platform.commoncode.application.CommonCodeService;
import com.hauly.platform.commoncode.application.command.CreateCodeCommand;
import com.hauly.platform.commoncode.application.command.UpdateCodeCommand;
import com.hauly.platform.commoncode.application.query.CommonCodeView;
import com.hauly.platform.commoncode.domain.model.CommonCode;
import com.hauly.platform.commoncode.domain.model.CommonCodeGroup;
import com.hauly.platform.commoncode.domain.model.CommonCodeId;
import com.hauly.platform.commoncode.domain.repository.CommonCodeGroupRepository;
import com.hauly.platform.commoncode.domain.repository.CommonCodeRepository;
import com.hauly.platform.support.exception.SystemProtectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonCodeServiceTest {

    @Mock
    private CommonCodeGroupRepository groupRepository;

    @Mock
    private CommonCodeRepository codeRepository;

    @InjectMocks
    private CommonCodeService service;

    private CommonCode activeCode;
    private CommonCode systemCode;

    @BeforeEach
    void setUp() {
        activeCode = CommonCode.create("FULFILLMENT_STATUS", "DRAFT", "작성 중", "Draft", "ร่าง", 10);
        systemCode = CommonCode.create("FULFILLMENT_STATUS", "REQUESTED", "의뢰", "Req", null, 20);
        // Simulate system=true by deactivating — we test system-protected via reflection or direct test below
    }

    @Test
    void getActiveCodes_returnsActiveCodesForGroup() {
        when(codeRepository.findActiveByGroupCode("FULFILLMENT_STATUS"))
                .thenReturn(List.of(activeCode));

        List<CommonCodeView> result = service.getActiveCodes("FULFILLMENT_STATUS");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("DRAFT");
        assertThat(result.get(0).name()).isEqualTo("작성 중");
    }

    @Test
    void createCode_validCommand_savesAndReturnsView() {
        CommonCodeGroup group = CommonCodeGroup.create("FULFILLMENT_STATUS", "처리상태", "Status", null);
        when(groupRepository.findByGroupCode("FULFILLMENT_STATUS")).thenReturn(Optional.of(group));
        when(codeRepository.save(any(CommonCode.class))).thenReturn(activeCode);

        CreateCodeCommand cmd = new CreateCodeCommand("FULFILLMENT_STATUS", "DRAFT", "작성 중", "Draft", null, 10);
        CommonCodeView view = service.createCode(cmd);

        assertThat(view.code()).isEqualTo("DRAFT");
        verify(codeRepository).save(any(CommonCode.class));
    }

    @Test
    void createCode_groupNotFound_throwsException() {
        when(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.empty());

        CreateCodeCommand cmd = new CreateCodeCommand("UNKNOWN", "X", "X", null, null, 0);
        assertThatThrownBy(() -> service.createCode(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void updateCode_found_updatesSuccessfully() {
        when(codeRepository.findById(any(CommonCodeId.class))).thenReturn(Optional.of(activeCode));
        when(codeRepository.save(any())).thenReturn(activeCode);

        UpdateCodeCommand cmd = new UpdateCodeCommand("FULFILLMENT_STATUS", "DRAFT", "작성 중", "Draft", null, null, null);
        CommonCodeView result = service.updateCode(cmd);

        assertThat(result.code()).isEqualTo("DRAFT");
    }

    @Test
    void deleteCode_systemCode_throwsSystemCodeProtectedException() {
        // Create a code that looks like system — use a mock
        CommonCode mockSystemCode = org.mockito.Mockito.mock(CommonCode.class);
        when(mockSystemCode.isSystem()).thenReturn(true);

        when(codeRepository.findById(any(CommonCodeId.class))).thenReturn(Optional.of(mockSystemCode));

        assertThatThrownBy(() -> service.deleteCode("FULFILLMENT_STATUS", "REQUESTED"))
                .isInstanceOf(SystemProtectedException.class);
    }

    @Test
    void deactivate_nonSystemCode_works() {
        when(codeRepository.findById(any(CommonCodeId.class))).thenReturn(Optional.of(activeCode));
        when(codeRepository.save(any())).thenReturn(activeCode);

        UpdateCodeCommand cmd = new UpdateCodeCommand("FULFILLMENT_STATUS", "DRAFT", null, null, null, null, false);
        service.updateCode(cmd);

        assertThat(activeCode.isActive()).isFalse();
    }
}
