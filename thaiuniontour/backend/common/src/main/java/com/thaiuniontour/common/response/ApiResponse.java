package com.thaiuniontour.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 공통 응답 래퍼 (04-api.md). Spring Data에 의존하지 않는다(범용).
 * 성공: { "success": true, "data": ... }
 * 목록: { "success": true, "data": [...], "page": { page, size, totalElements, totalPages } }
 * 실패: { "success": false, "error": { "code": ..., "message": ... } }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, PageMeta page, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<List<T>> page(List<T> content, PageMeta page) {
        return new ApiResponse<>(true, content, page, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, null, new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {}

    public record PageMeta(int page, int size, long totalElements, int totalPages) {}
}
