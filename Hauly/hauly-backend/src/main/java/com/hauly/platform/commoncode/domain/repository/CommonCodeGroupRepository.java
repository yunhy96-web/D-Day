package com.hauly.platform.commoncode.domain.repository;

import com.hauly.platform.commoncode.domain.model.CommonCodeGroup;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for CommonCodeGroup.
 * Plain Java — no Spring/JPA imports.
 */
public interface CommonCodeGroupRepository {

    Optional<CommonCodeGroup> findByGroupCode(String groupCode);

    List<CommonCodeGroup> findAll();

    List<CommonCodeGroup> findAllActive();

    CommonCodeGroup save(CommonCodeGroup group);

    void deleteByGroupCode(String groupCode);
}
