package com.checkstock.dto;

import lombok.Data;

@Data
public class ProductRequest {
    private String name;
    private String url;
    private String sizeSelector = "li.variations-attribute";
    private String soldOutIndicator = "out";
    private String targetSize; // 클릭할 허리 사이즈 (예: "32")
    private int refreshInterval = 30;
}
