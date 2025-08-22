package com.commerce.product.application.usecase;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateProductResponse {
    private final String productId;
    private final String name;
    private final String description;
    private final String type;
    private final String status;
}