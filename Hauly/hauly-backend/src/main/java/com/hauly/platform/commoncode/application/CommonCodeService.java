package com.hauly.platform.commoncode.application;

import com.hauly.platform.commoncode.application.command.CreateCodeCommand;
import com.hauly.platform.commoncode.application.command.UpdateCodeCommand;
import com.hauly.platform.commoncode.application.query.CommonCodeGroupView;
import com.hauly.platform.commoncode.application.query.CommonCodeView;
import com.hauly.platform.commoncode.domain.model.CommonCode;
import com.hauly.platform.commoncode.domain.model.CommonCodeId;
import com.hauly.shared.kernel.Lang;
import com.hauly.platform.commoncode.domain.repository.CommonCodeGroupRepository;
import com.hauly.platform.commoncode.domain.repository.CommonCodeRepository;
import com.hauly.platform.support.exception.SystemProtectedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for common code operations.
 * Orchestrates domain + infrastructure; owns transaction boundaries.
 */
@Service
@Transactional
public class CommonCodeService {

    private final CommonCodeGroupRepository groupRepository;
    private final CommonCodeRepository codeRepository;

    public CommonCodeService(CommonCodeGroupRepository groupRepository,
                             CommonCodeRepository codeRepository) {
        this.groupRepository = groupRepository;
        this.codeRepository = codeRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "commonCode.byGroup", key = "#groupCode + '_' + #lang.name()")
    public List<CommonCodeView> getActiveCodes(String groupCode, Lang lang) {
        return codeRepository.findActiveByGroupCode(groupCode).stream()
                .map(c -> CommonCodeView.from(c, lang))
                .toList();
    }

    /** Convenience overload — delegates to the canonical two-arg form (no separate cache entry). */
    @Transactional(readOnly = true)
    public List<CommonCodeView> getActiveCodes(String groupCode) {
        return getActiveCodes(groupCode, Lang.KO);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "commonCode.byCode", key = "#groupCode + '_' + #code")
    public CommonCodeView getCode(String groupCode, String code) {
        return codeRepository.findById(new CommonCodeId(groupCode, code))
                .map(c -> CommonCodeView.from(c, Lang.KO))
                .orElseThrow(() -> new IllegalArgumentException("Code not found: " + groupCode + "." + code));
    }

    @Transactional(readOnly = true)
    public List<CommonCodeGroupView> listGroups() {
        return groupRepository.findAll().stream()
                .map(CommonCodeGroupView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommonCodeView> listAllCodesByGroup(String groupCode) {
        return codeRepository.findByGroupCode(groupCode).stream()
                .map(c -> CommonCodeView.from(c, Lang.KO))
                .toList();
    }

    @Caching(evict = {
        @CacheEvict(value = "commonCode.byGroup", key = "#command.groupCode()"),
        @CacheEvict(value = "commonCode.byGroup", allEntries = true)
    })
    public CommonCodeView createCode(CreateCodeCommand command) {
        // Validate group exists
        groupRepository.findByGroupCode(command.groupCode())
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + command.groupCode()));

        CommonCode cc = CommonCode.create(
                command.groupCode(), command.code(),
                command.nameKo(), command.nameEn(), command.nameTh(), command.sortOrder()
        );
        return CommonCodeView.from(codeRepository.save(cc), Lang.KO);
    }

    @Caching(evict = {
        @CacheEvict(value = "commonCode.byGroup", allEntries = true),
        @CacheEvict(value = "commonCode.byCode", key = "#command.groupCode() + '_' + #command.code()")
    })
    public CommonCodeView updateCode(UpdateCodeCommand command) {
        CommonCode cc = codeRepository.findById(new CommonCodeId(command.groupCode(), command.code()))
                .orElseThrow(() -> new IllegalArgumentException("Code not found: " + command.groupCode() + "." + command.code()));

        if (command.nameKo() != null) {
            cc.updateNames(command.nameKo(), command.nameEn(), command.nameTh());
        }
        if (command.sortOrder() != null) {
            cc.updateSortOrder(command.sortOrder());
        }
        if (command.active() != null) {
            if (command.active()) cc.activate();
            else cc.deactivate();
        }
        return CommonCodeView.from(codeRepository.save(cc), Lang.KO);
    }

    @Caching(evict = {
        @CacheEvict(value = "commonCode.byGroup", allEntries = true),
        @CacheEvict(value = "commonCode.byCode", key = "#groupCode + '_' + #code")
    })
    public void deleteCode(String groupCode, String code) {
        CommonCode cc = codeRepository.findById(new CommonCodeId(groupCode, code))
                .orElseThrow(() -> new IllegalArgumentException("Code not found: " + groupCode + "." + code));
        if (cc.isSystem()) {
            throw new SystemProtectedException("Cannot delete system code: " + groupCode + "." + code);
        }
        codeRepository.deleteById(new CommonCodeId(groupCode, code));
    }
}
