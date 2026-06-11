package com.thaiuniontour.admin.catalog;

import com.thaiuniontour.admin.catalog.dto.ProductFormDto;
import com.thaiuniontour.domain.catalog.model.Product;
import com.thaiuniontour.domain.catalog.model.ProductCategory;
import com.thaiuniontour.domain.catalog.repository.ProductCategoryRepository;
import com.thaiuniontour.domain.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public AdminProductService(ProductRepository productRepository,
                               ProductCategoryRepository productCategoryRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Transactional
    public Product create(ProductFormDto dto) {
        Product product = Product.builder()
                .name(dto.getName())
                .summary(dto.getSummary())
                .description(dto.getDescription())
                .stockType(dto.getStockType())
                .basePrice(dto.getBasePrice())
                .status(dto.getStatus())
                .thumbnailUrl(dto.getThumbnailUrl())
                .build();
        product = productRepository.save(product);

        saveCategories(product.getId(), dto);
        return product;
    }

    @Transactional
    public Product update(UUID id, ProductFormDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + id));
        product.update(dto.getName(), dto.getSummary(), dto.getDescription(),
                dto.getBasePrice(), dto.getStatus(), dto.getThumbnailUrl());
        product = productRepository.save(product);

        productCategoryRepository.deleteByIdProductId(id);
        saveCategories(id, dto);
        return product;
    }

    private void saveCategories(UUID productId, ProductFormDto dto) {
        productCategoryRepository.save(
                new ProductCategory(productId, dto.getThemeCategoryId(), true));

        if (dto.getDestinationCategoryId() != null) {
            productCategoryRepository.save(
                    new ProductCategory(productId, dto.getDestinationCategoryId(), false));
        }
    }
}
