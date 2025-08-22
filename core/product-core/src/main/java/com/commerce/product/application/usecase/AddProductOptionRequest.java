package com.commerce.product.application.usecase;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class AddProductOptionRequest {
    private final String productId;
    private final String optionName;
    private final BigDecimal price;
    private final String currency;
    private final Map<String, Integer> skuMappings;
}