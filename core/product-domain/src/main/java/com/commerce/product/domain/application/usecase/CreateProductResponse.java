package com.commerce.product.domain.application.usecase;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProductResponse {
    private final String productId;
    private final String name;
    private final String description;
    private final ProductType type;
    private final ProductStatus status;
}