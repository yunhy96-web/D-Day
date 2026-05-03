package com.hauly.platform.commoncode.infrastructure.persistence;

import com.hauly.platform.commoncode.domain.model.CommonCodeGroup;
import com.hauly.platform.commoncode.domain.repository.CommonCodeGroupRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA implementation of CommonCodeGroupRepository.
 */
@Repository
public interface JpaCommonCodeGroupRepository
        extends JpaRepository<CommonCodeGroup, String>, CommonCodeGroupRepository {

    @Override
    Optional<CommonCodeGroup> findByGroupCode(String groupCode);

    @Override
    @Query("SELECT g FROM CommonCodeGroup g WHERE g.active = true ORDER BY g.groupCode")
    List<CommonCodeGroup> findAllActive();

    @Override
    default void deleteByGroupCode(String groupCode) {
        deleteById(groupCode);
    }
}
