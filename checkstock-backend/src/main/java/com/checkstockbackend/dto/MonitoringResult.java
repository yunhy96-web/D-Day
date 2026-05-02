package com.checkstockbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MonitoringResult {
    private int totalCount;
    private int matchedCount;
    private List<ProductDto> matched;
}
