package com.thaiuniontour.api.catalog;

import com.thaiuniontour.api.catalog.dto.CategoryDto;
import com.thaiuniontour.api.catalog.dto.CategoryLandingDto;
import com.thaiuniontour.api.catalog.service.CategoryQueryService;
import com.thaiuniontour.common.response.ApiResponse;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    public CategoryController(CategoryQueryService categoryQueryService) {
        this.categoryQueryService = categoryQueryService;
    }

    /** GET /api/v1/categories?type=THEME|DESTINATION → 중첩 트리. */
    @GetMapping
    public ApiResponse<List<CategoryDto>> tree(@RequestParam(defaultValue = "THEME") CategoryType type) {
        return ApiResponse.ok(categoryQueryService.tree(type));
    }

    /** GET /api/v1/categories/by-slug?type=THEME&slug=ayutthaya → 랜딩(브레드크럼+하위). */
    @GetMapping("/by-slug")
    public ApiResponse<CategoryLandingDto> bySlug(
            @RequestParam(defaultValue = "THEME") CategoryType type,
            @RequestParam String slug) {
        return ApiResponse.ok(categoryQueryService.landing(type, slug));
    }
}
