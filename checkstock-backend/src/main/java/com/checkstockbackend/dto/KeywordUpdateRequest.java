package com.checkstockbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KeywordUpdateRequest {
    private List<String> includeKeywords;
    private List<String> excludeKeywords;
}
