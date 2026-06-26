package com.thaiuniontour.api.catalog;

import com.thaiuniontour.api.catalog.dto.ProductDto.ResProductDetail;
import com.thaiuniontour.api.catalog.dto.ProductDto.ResProductList;
import com.thaiuniontour.api.catalog.service.CategoryQueryService;
import com.thaiuniontour.api.catalog.service.ProductQueryService;
import com.thaiuniontour.api.catalog.service.ProductQueryService.SearchCond;
import com.thaiuniontour.common.response.ApiResponse;
import com.thaiuniontour.common.response.ApiResponse.PageMeta;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductQueryService productQueryService;
    private final CategoryQueryService categoryQueryService;

    public ProductController(ProductQueryService productQueryService,
                             CategoryQueryService categoryQueryService) {
        this.productQueryService = productQueryService;
        this.categoryQueryService = categoryQueryService;
    }

    /**
     * GET /api/v1/products?q=&categoryId=&destinationId=&minPrice=&maxPrice=&sort=&page=&size=
     * categoryId는 서브트리로 확장(상위 클릭 시 하위 포함). 판매중만 노출.
     */
    @GetMapping
    public ApiResponse<List<ResProductList>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID destinationId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Set<UUID> categoryIds = categoryId == null ? null : categoryQueryService.subtreeIds(categoryId);
        SearchCond cond = new SearchCond(q, categoryIds, destinationId, minPrice, maxPrice, sort);

        Page<ResProductList> result = productQueryService.search(cond, page, size);
        return ApiResponse.page(result.getContent(),
                new PageMeta(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** GET /api/v1/products/{id} → 상세(카테고리·브레드크럼 포함). */
    @GetMapping("/{id}")
    public ApiResponse<ResProductDetail> detail(@PathVariable UUID id) {
        return ApiResponse.ok(productQueryService.detail(id));
    }

    /** GET /api/v1/products/{id}/schedules → 날짜별 재고/가격(예약 캘린더). */
    @GetMapping("/{id}/schedules")
    public ApiResponse<List<com.thaiuniontour.api.catalog.dto.ProductDto.ResSchedule>> schedules(
            @PathVariable UUID id) {
        return ApiResponse.ok(productQueryService.schedules(id));
    }
}
