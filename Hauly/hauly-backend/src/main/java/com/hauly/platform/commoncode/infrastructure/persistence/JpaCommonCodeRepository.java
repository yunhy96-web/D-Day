package com.hauly.platform.commoncode.infrastructure.persistence;

import com.hauly.platform.commoncode.domain.model.CommonCode;
import com.hauly.platform.commoncode.domain.model.CommonCodeId;
import com.hauly.platform.commoncode.domain.repository.CommonCodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA implementation of CommonCodeRepository.
 * findById and deleteById are already provided by JpaRepository<CommonCode, CommonCodeId>.
 */
@Repository
public interface JpaCommonCodeRepository
        extends JpaRepository<CommonCode, CommonCodeId>, CommonCodeRepository {

    @Query("SELECT c FROM CommonCode c WHERE c.groupCode = :groupCode ORDER BY c.sortOrder, c.code")
    List<CommonCode> findByGroupCode(@Param("groupCode") String groupCode);

    @Query("SELECT c FROM CommonCode c WHERE c.groupCode = :groupCode AND c.active = true ORDER BY c.sortOrder, c.code")
    List<CommonCode> findActiveByGroupCode(@Param("groupCode") String groupCode);
}
