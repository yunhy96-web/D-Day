package com.checkstockbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CheckNowResponse {
    private int totalCount;
    private int matchedCount;
    private List<ProductDto> matched;
    private LocalDateTime checkedAt;
}
