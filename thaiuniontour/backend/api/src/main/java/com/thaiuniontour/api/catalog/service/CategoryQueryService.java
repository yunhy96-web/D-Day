package com.thaiuniontour.api.catalog.service;

import com.thaiuniontour.api.catalog.dto.CategoryDto;
import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import com.thaiuniontour.domain.catalog.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카테고리 트리 조회 (경량 읽기 서비스). */
@Service
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;

    public CategoryQueryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** type별 전체를 로드해 parentId 기준 중첩 트리로 조립. */
    @Transactional(readOnly = true)
    public List<CategoryDto> tree(CategoryType type) {
        List<Category> all = categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(type);
        Map<UUID, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? ROOT : c.getParentId()));
        return build(byParent, ROOT);
    }

    private static final UUID ROOT = new UUID(0L, 0L);

    private List<CategoryDto> build(Map<UUID, List<Category>> byParent, UUID parentKey) {
        List<Category> children = byParent.getOrDefault(parentKey, List.of());
        List<CategoryDto> result = new ArrayList<>(children.size());
        for (Category c : children) {
            List<CategoryDto> sub = build(byParent, c.getId());
            result.add(new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getLevel(),
                    sub.isEmpty() ? null : sub));
        }
        return result;
    }
}
