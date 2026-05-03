package com.hauly.platform.category.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.category.application.command.CreateCategoryCommand;
import com.hauly.platform.category.application.query.CategoryDetailView;
import com.hauly.platform.category.application.query.CategoryView;
import com.hauly.platform.category.domain.model.FieldDefinition;
import com.hauly.platform.category.domain.model.ProductCategory;
import com.hauly.platform.category.domain.model.ValidationResult;
import com.hauly.platform.category.domain.repository.ProductCategoryRepository;
import com.hauly.platform.category.domain.service.CategorySchemaValidator;
import com.hauly.platform.category.domain.service.InvalidCategoryAttributesException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for product category operations.
 */
@Service
@Transactional
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final CategorySchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;

    public CategoryService(ProductCategoryRepository categoryRepository, ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.schemaValidator = new CategorySchemaValidator(objectMapper);
    }

    @Transactional(readOnly = true)
    @Cacheable("category.list")
    public List<CategoryView> getActiveCategories() {
        return categoryRepository.findAllActive().stream()
                .map(c -> CategoryView.from(c, schemaValidator.parseSchema(c.getFieldSchema())))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "category.byCode", key = "#code")
    public CategoryDetailView getCategory(String code) {
        ProductCategory cat = categoryRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + code));
        List<FieldDefinition> fields = schemaValidator.parseSchema(cat.getFieldSchema());
        return CategoryDetailView.from(cat, fields);
    }

    public void validateAttributes(String categoryCode, JsonNode attributes) {
        ProductCategory cat = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryCode));
        ValidationResult result = schemaValidator.validate(cat, attributes);
        if (!result.isValid()) {
            throw new InvalidCategoryAttributesException(result.errors());
        }
    }

    @CacheEvict(value = "category.list", allEntries = true)
    public CategoryView createCategory(CreateCategoryCommand command) {
        ProductCategory cat = ProductCategory.create(
                command.code(), command.nameKey(), command.fieldSchema(), command.sortOrder());
        ProductCategory saved = categoryRepository.save(cat);
        return CategoryView.from(saved, schemaValidator.parseSchema(saved.getFieldSchema()));
    }

    @CacheEvict(value = {"category.list", "category.byCode"}, allEntries = true)
    public CategoryDetailView updateSchema(String code, String newSchema) {
        ProductCategory cat = categoryRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + code));
        // basic meta-validation: must be parseable JSON with a "fields" array
        validateSchemaStructure(newSchema);
        cat.updateSchema(newSchema);
        categoryRepository.save(cat);
        List<FieldDefinition> fields = schemaValidator.parseSchema(newSchema);
        return CategoryDetailView.from(cat, fields);
    }

    private void validateSchemaStructure(String schemaJson) {
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            if (!root.isObject() || !root.has("fields") || !root.get("fields").isArray()) {
                throw new IllegalArgumentException("Schema must be a JSON object with a 'fields' array");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema JSON: " + e.getMessage());
        }
    }
}
