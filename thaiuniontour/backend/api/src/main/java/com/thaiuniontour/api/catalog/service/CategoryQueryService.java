package com.thaiuniontour.api.catalog.service;

import com.thaiuniontour.api.catalog.dto.CategoryDto;
import com.thaiuniontour.api.catalog.dto.CategoryLandingDto;
import com.thaiuniontour.common.error.ErrorCode;
import com.thaiuniontour.common.exception.BusinessException;
import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import com.thaiuniontour.domain.catalog.repository.CategoryRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** slug → 랜딩 정보(브레드크럼 조상 + 직계 하위). /c/[...slug] 라우트용. */
    @Transactional(readOnly = true)
    public CategoryLandingDto landing(CategoryType type, String slug) {
        Category target = categoryRepository.findByTypeAndSlug(type, slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "카테고리를 찾을 수 없습니다: " + slug));

        Map<UUID, Category> byId = categoryRepository.findByType(type).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // 브레드크럼: 대상에서 루트까지 올라간 뒤 뒤집기
        List<CategoryLandingDto.Ref> crumb = new ArrayList<>();
        Category cur = target;
        int guard = 0;
        while (cur != null && guard++ < 5) {
            crumb.add(new CategoryLandingDto.Ref(cur.getId(), cur.getName(), cur.getSlug()));
            cur = cur.getParentId() == null ? null : byId.get(cur.getParentId());
        }
        Collections.reverse(crumb);

        // 직계 하위(use_yn=true, sort)
        List<CategoryLandingDto.Ref> children = byId.values().stream()
                .filter(c -> target.getId().equals(c.getParentId()) && c.isUseYn())
                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
                .map(c -> new CategoryLandingDto.Ref(c.getId(), c.getName(), c.getSlug()))
                .toList();

        return new CategoryLandingDto(target.getId(), target.getName(), target.getSlug(),
                target.getLevel(), crumb, children);
    }

    /** 카테고리 id의 서브트리(자기 + 모든 자손) id 집합. 상품 필터(상위 클릭 시 하위 포함)용. */
    @Transactional(readOnly = true)
    public Set<UUID> subtreeIds(UUID categoryId) {
        Category root = categoryRepository.findById(categoryId).orElse(null);
        if (root == null) return Set.of(categoryId);

        Map<UUID, List<Category>> byParent = categoryRepository.findByType(root.getType()).stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        Set<UUID> ids = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(categoryId);
        while (!stack.isEmpty()) {
            UUID id = stack.pop();
            if (!ids.add(id)) continue;
            for (Category child : byParent.getOrDefault(id, List.of())) {
                stack.push(child.getId());
            }
        }
        return ids;
    }
}
