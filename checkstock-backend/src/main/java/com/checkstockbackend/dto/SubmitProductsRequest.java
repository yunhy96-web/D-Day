package com.checkstockbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitProductsRequest {
    private List<ProductDto> products;
}
