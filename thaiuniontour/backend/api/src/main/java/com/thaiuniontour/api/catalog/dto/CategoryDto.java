package com.thaiuniontour.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/** 카테고리 응답 (트리 노드). children 중첩으로 메가메뉴 렌더. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryDto(
        UUID id,
        String name,
        String slug,
        int level,
        List<CategoryDto> children
) {}
