package com.thaiuniontour.domain.catalog.repository;

import com.thaiuniontour.domain.catalog.model.ProductCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategory.Pk> {

    List<ProductCategory> findByIdProductId(UUID productId);

    void deleteByIdProductId(UUID productId);
}
