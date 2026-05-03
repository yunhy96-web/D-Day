package com.hauly.platform.category.infrastructure.persistence;

import com.hauly.platform.category.domain.model.ProductCategory;
import com.hauly.platform.category.domain.repository.ProductCategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA implementation of ProductCategoryRepository.
 */
@Repository
public interface JpaProductCategoryRepository
        extends JpaRepository<ProductCategory, Long>, ProductCategoryRepository {

    Optional<ProductCategory> findByCode(String code);

    @Query("SELECT c FROM ProductCategory c WHERE c.active = true ORDER BY c.sortOrder, c.code")
    List<ProductCategory> findAllActive();
}
