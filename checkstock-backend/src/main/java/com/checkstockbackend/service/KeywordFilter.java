package com.checkstockbackend.service;

import com.checkstockbackend.dto.ProductDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeywordFilter {

    public List<ProductDto> filter(List<ProductDto> products,
                                   List<String> include,
                                   List<String> exclude) {
        List<String> inc = include == null ? List.of() : include;
        List<String> exc = exclude == null ? List.of() : exclude;

        return products.stream()
                .filter(p -> inc.isEmpty() || inc.stream().anyMatch(k -> p.getName().contains(k)))
                .filter(p -> exc.stream().noneMatch(k -> p.getName().contains(k)))
                .toList();
    }
}
