package com.thaiuniontour.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/** 상품 응답 DTO 모음. */
public final class ProductDto {

    private ProductDto() {}

    /** 목록 카드용 요약. */
    public record ResProductList(
            UUID id,
            String name,
            String summary,
            String thumbnailUrl,
            long basePrice,
            String status
    ) {}

    /** 상세. categories(테마/지역) + breadcrumb(대표 테마 경로). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResProductDetail(
            UUID id,
            String name,
            String summary,
            String description,
            String stockType,
            long basePrice,
            String status,
            String thumbnailUrl,
            Categories categories,
            List<String> breadcrumb
    ) {
        public record Categories(List<Ref> theme, List<Ref> destination) {}
        public record Ref(UUID id, String name) {}
    }

    /** 날짜별 재고/가격 (예약 캘린더). stock=null → 무제한. */
    public record ResSchedule(
            UUID scheduleId,
            String saleDate,
            Integer stock,
            long price,
            String status,
            boolean soldOut
    ) {}
}
