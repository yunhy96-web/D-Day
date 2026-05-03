package com.hauly.platform.commoncode.domain.repository;

import com.hauly.platform.commoncode.domain.model.CommonCode;
import com.hauly.platform.commoncode.domain.model.CommonCodeId;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for CommonCode.
 * Plain Java — no Spring/JPA imports.
 */
public interface CommonCodeRepository {

    Optional<CommonCode> findById(CommonCodeId id);

    List<CommonCode> findByGroupCode(String groupCode);

    List<CommonCode> findActiveByGroupCode(String groupCode);

    CommonCode save(CommonCode code);

    void deleteById(CommonCodeId id);
}
