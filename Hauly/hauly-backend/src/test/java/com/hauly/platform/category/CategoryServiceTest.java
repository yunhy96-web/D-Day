package com.hauly.platform.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.category.application.CategoryService;
import com.hauly.platform.category.application.query.CategoryDetailView;
import com.hauly.platform.category.application.query.CategoryView;
import com.hauly.platform.category.domain.model.ProductCategory;
import com.hauly.platform.category.domain.repository.ProductCategoryRepository;
import com.hauly.platform.category.domain.service.InvalidCategoryAttributesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private ProductCategoryRepository categoryRepository;

    private CategoryService service;
    private ObjectMapper objectMapper;

    private static final String LENS_SCHEMA = """
            {"fields": [{"key": "brand", "label_key": "k", "type": "text", "required": true}]}
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new CategoryService(categoryRepository, objectMapper);
    }

    @Test
    void getActiveCategories_returnsAllActive() {
        ProductCategory cat = ProductCategory.create("CONTACT_LENS", "category.contact_lens.name", LENS_SCHEMA, 20);
        when(categoryRepository.findAllActive()).thenReturn(List.of(cat));

        List<CategoryView> result = service.getActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("CONTACT_LENS");
    }

    @Test
    void getCategory_found_returnsParsedSchema() {
        ProductCategory cat = ProductCategory.create("CONTACT_LENS", "category.contact_lens.name", LENS_SCHEMA, 20);
        when(categoryRepository.findByCode("CONTACT_LENS")).thenReturn(Optional.of(cat));

        CategoryDetailView detail = service.getCategory("CONTACT_LENS");

        assertThat(detail.code()).isEqualTo("CONTACT_LENS");
        assertThat(detail.fields()).hasSize(1);
        assertThat(detail.fields().get(0).key()).isEqualTo("brand");
    }

    @Test
    void getCategory_notFound_throwsIllegalArgument() {
        when(categoryRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategory("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void validateAttributes_valid_doesNotThrow() throws Exception {
        ProductCategory cat = ProductCategory.create("CONTACT_LENS", "category.contact_lens.name", LENS_SCHEMA, 20);
        when(categoryRepository.findByCode("CONTACT_LENS")).thenReturn(Optional.of(cat));

        service.validateAttributes("CONTACT_LENS", objectMapper.readTree("{\"brand\": \"Acuvue\"}"));
        // no exception
    }

    @Test
    void validateAttributes_missing_required_throwsInvalidCategoryAttributesException() throws Exception {
        ProductCategory cat = ProductCategory.create("CONTACT_LENS", "category.contact_lens.name", LENS_SCHEMA, 20);
        when(categoryRepository.findByCode("CONTACT_LENS")).thenReturn(Optional.of(cat));

        assertThatThrownBy(() -> service.validateAttributes("CONTACT_LENS", objectMapper.readTree("{}")))
                .isInstanceOf(InvalidCategoryAttributesException.class);
    }

    @Test
    void updateSchema_invalidJson_throwsIllegalArgument() {
        ProductCategory cat = ProductCategory.create("CONTACT_LENS", "category.contact_lens.name", LENS_SCHEMA, 20);
        when(categoryRepository.findByCode("CONTACT_LENS")).thenReturn(Optional.of(cat));

        assertThatThrownBy(() -> service.updateSchema("CONTACT_LENS", "{\"noFields\": []}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
