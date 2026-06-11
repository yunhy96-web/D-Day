package com.thaiuniontour.api.catalog;

import com.thaiuniontour.api.catalog.dto.ProductDto.ResProductDetail;
import com.thaiuniontour.api.catalog.dto.ProductDto.ResProductList;
import com.thaiuniontour.api.catalog.service.ProductQueryService;
import com.thaiuniontour.common.response.ApiResponse;
import com.thaiuniontour.common.response.ApiResponse.PageMeta;
import java.util.List;
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

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    /** GET /api/v1/products?q=&categoryId=&sort=&page=&size= → 검색/필터 목록(판매중). */
    @GetMapping
    public ApiResponse<List<ResProductList>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ResProductList> result = productQueryService.search(q, categoryId, sort, page, size);
        return ApiResponse.page(result.getContent(),
                new PageMeta(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** GET /api/v1/products/{id} → 상세(카테고리·브레드크럼 포함). */
    @GetMapping("/{id}")
    public ApiResponse<ResProductDetail> detail(@PathVariable UUID id) {
        return ApiResponse.ok(productQueryService.detail(id));
    }
}
