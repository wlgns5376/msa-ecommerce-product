package com.commerce.product.domain.application.usecase;

import com.commerce.product.domain.model.ProductType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProductRequest {
    private final String name;
    private final String description;
    private final ProductType type;
}