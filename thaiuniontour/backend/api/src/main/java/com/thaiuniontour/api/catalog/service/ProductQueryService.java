package com.thaiuniontour.api.catalog.service;

import com.thaiuniontour.api.catalog.dto.ProductDto.ResProductDetail;
import com.thaiuniontour.api.catalog.dto.ProductDto.ResProductList;
import com.thaiuniontour.api.catalog.dto.ProductDto.ResSchedule;
import com.thaiuniontour.domain.catalog.model.ScheduleStatus;
import com.thaiuniontour.domain.catalog.repository.ProductScheduleRepository;
import com.thaiuniontour.common.error.ErrorCode;
import com.thaiuniontour.common.exception.BusinessException;
import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import com.thaiuniontour.domain.catalog.model.Product;
import com.thaiuniontour.domain.catalog.model.ProductCategory;
import com.thaiuniontour.domain.catalog.model.ProductStatus;
import com.thaiuniontour.domain.catalog.repository.CategoryRepository;
import com.thaiuniontour.domain.catalog.repository.ProductCategoryRepository;
import com.thaiuniontour.domain.catalog.repository.ProductRepository;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상품 검색/상세 조회 (경량 읽기 서비스). */
@Service
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ProductScheduleRepository scheduleRepository;

    public ProductQueryService(ProductRepository productRepository,
                               ProductCategoryRepository productCategoryRepository,
                               CategoryRepository categoryRepository,
                               ProductScheduleRepository scheduleRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /** 날짜별 재고/가격(예약 캘린더). 매진/마감 판정 포함. */
    @Transactional(readOnly = true)
    public List<ResSchedule> schedules(UUID productId) {
        return scheduleRepository.findByProductIdOrderBySaleDateAsc(productId).stream()
                .map(s -> new ResSchedule(
                        s.getId(), s.getSaleDate().toString(), s.getStock(), s.getPrice(),
                        s.getStatus().name(),
                        s.getStatus() != ScheduleStatus.OPEN
                                || (s.getStock() != null && s.getStock() <= 0)))
                .toList();
    }

    /** 단순 검색(홈/검색바). categoryId는 정확 일치. */
    @Transactional(readOnly = true)
    public Page<ResProductList> search(String q, UUID categoryId, String sort, int page, int size) {
        return search(new SearchCond(q, categoryId != null ? Set.of(categoryId) : null,
                null, null, null, sort), page, size);
    }

    /**
     * 카테고리 랜딩/필터 검색. categoryIds(서브트리), destinationId(지역 패싯), 가격범위 결합.
     * 해당하는 조건만 Specification으로 추가 → null 파라미터 SQL 문제 없음.
     */
    @Transactional(readOnly = true)
    public Page<ResProductList> search(SearchCond cond, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), toSort(cond.sort()));

        // 고객 노출은 판매중(ON_SALE)만.
        Specification<Product> spec = (root, query, cb) ->
                cb.equal(root.get("status"), ProductStatus.ON_SALE);

        String qn = blankToNull(cond.q());
        if (qn != null) {
            String like = "%" + qn.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (cond.categoryIds() != null && !cond.categoryIds().isEmpty()) {
            spec = spec.and(inCategory(cond.categoryIds()));
        }
        if (cond.destinationId() != null) {
            spec = spec.and(inCategory(Set.of(cond.destinationId())));
        }
        if (cond.minPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.ge(root.get("basePrice"), cond.minPrice()));
        }
        if (cond.maxPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.le(root.get("basePrice"), cond.maxPrice()));
        }

        return productRepository.findAll(spec, pageable)
                .map(p -> new ResProductList(p.getId(), p.getName(), p.getSummary(),
                        p.getThumbnailUrl(), p.getBasePrice(), p.getStatus().name()));
    }

    /** product_category에 주어진 카테고리들 중 하나라도 태깅된 상품. */
    private Specification<Product> inCategory(Set<UUID> categoryIds) {
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            var pc = sub.from(ProductCategory.class);
            sub.select(pc.get("id").get("productId"));
            sub.where(pc.get("id").get("categoryId").in(categoryIds));
            return root.get("id").in(sub);
        };
    }

    /** 검색 조건. categoryIds=테마 서브트리(상위 클릭 시 하위 포함). */
    public record SearchCond(String q, Set<UUID> categoryIds, UUID destinationId,
                             Long minPrice, Long maxPrice, String sort) {}

    @Transactional(readOnly = true)
    public ResProductDetail detail(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "상품을 찾을 수 없습니다."));

        List<ProductCategory> links = productCategoryRepository.findByIdProductId(id);
        Map<UUID, Category> catById = categoryRepository.findAllById(
                        links.stream().map(ProductCategory::getCategoryId).toList())
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        List<ResProductDetail.Ref> theme = new ArrayList<>();
        List<ResProductDetail.Ref> destination = new ArrayList<>();
        UUID primaryThemeId = null;
        for (ProductCategory link : links) {
            Category c = catById.get(link.getCategoryId());
            if (c == null) continue;
            if (c.getType() == CategoryType.THEME) {
                theme.add(new ResProductDetail.Ref(c.getId(), c.getName()));
                if (link.isPrimary()) primaryThemeId = c.getId();
            } else {
                destination.add(new ResProductDetail.Ref(c.getId(), c.getName()));
            }
        }

        return new ResProductDetail(
                p.getId(), p.getName(), p.getSummary(), p.getDescription(),
                p.getStockType().name(), p.getBasePrice(), p.getStatus().name(), p.getThumbnailUrl(),
                new ResProductDetail.Categories(theme, destination),
                breadcrumb(primaryThemeId));
    }

    /** 대표 테마 카테고리에서 루트까지 이름 경로(3단계). */
    private List<String> breadcrumb(UUID primaryThemeId) {
        if (primaryThemeId == null) return List.of();
        List<String> names = new ArrayList<>();
        UUID cursor = primaryThemeId;
        int guard = 0;
        while (cursor != null && guard++ < 5) {
            Category c = categoryRepository.findById(cursor).orElse(null);
            if (c == null) break;
            names.add(c.getName());
            cursor = c.getParentId();
        }
        Collections.reverse(names);
        return names;
    }

    private Sort toSort(String sort) {
        return switch (sort == null ? "latest" : sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // latest
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
