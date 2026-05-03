package com.hauly.platform.category.domain.repository;

import com.hauly.platform.category.domain.model.ProductCategory;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for ProductCategory.
 * Plain Java — no Spring/JPA imports.
 */
public interface ProductCategoryRepository {

    Optional<ProductCategory> findByCode(String code);

    List<ProductCategory> findAllActive();

    List<ProductCategory> findAll();

    ProductCategory save(ProductCategory category);
}
