package com.thaiuniontour.api.catalog.dto;

import java.util.List;
import java.util.UUID;

/** 카테고리 랜딩(/c/[...slug]) 해석 결과: 대상 + 브레드크럼(조상) + 하위(서브 네비). */
public record CategoryLandingDto(
        UUID id,
        String name,
        String slug,
        int level,
        List<Ref> breadcrumb,   // 루트 → 대상 (대상 포함)
        List<Ref> children      // 직계 하위(서브카테고리 칩)
) {
    public record Ref(UUID id, String name, String slug) {}
}
