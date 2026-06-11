package com.thaiuniontour.domain.catalog.repository;

import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** type별 전체(트리 조립용). use_yn=true만. sort 순. */
    List<Category> findByTypeAndUseYnTrueOrderBySortAsc(CategoryType type);
}
