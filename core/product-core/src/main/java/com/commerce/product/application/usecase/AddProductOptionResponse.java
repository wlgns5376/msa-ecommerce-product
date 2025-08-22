package com.commerce.product.application.usecase;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddProductOptionResponse {
    private final String productId;
    private final String optionId;
    private final String optionName;
}