package com.thaiuniontour.domain.catalog.repository;

import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** type별 전체(트리 조립용). use_yn=true만. sort 순. */
    List<Category> findByTypeAndUseYnTrueOrderBySortAsc(CategoryType type);

    /** type별 전체(서브트리 계산용, use_yn 무관). */
    List<Category> findByType(CategoryType type);

    /** slug로 카테고리 해석(type 내 slug 유일). 랜딩 라우트(/c/[...slug])용. */
    Optional<Category> findByTypeAndSlug(CategoryType type, String slug);
}
