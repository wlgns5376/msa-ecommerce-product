package com.commerce.product.domain.application.usecase;

import com.commerce.product.domain.model.Product;
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
    
    public static CreateProductResponse from(Product product) {
        return CreateProductResponse.builder()
                .productId(product.getId().toString())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType())
                .status(product.getStatus())
                .build();
    }
}
