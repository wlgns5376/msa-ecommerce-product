package com.commerce.product.api.adapter.in.web.dto;

import com.commerce.product.application.usecase.CreateProductResponse;
import com.commerce.product.application.usecase.GetProductResponse;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductResponse {
    
    private final String productId;
    private final String name;
    private final String description;
    private final ProductType type;
    private final ProductStatus status;
    
    public static ProductResponse from(CreateProductResponse useCaseResponse) {
        return ProductResponse.builder()
                .productId(useCaseResponse.getProductId())
                .name(useCaseResponse.getName())
                .description(useCaseResponse.getDescription())
                .type(useCaseResponse.getType())
                .status(useCaseResponse.getStatus())
                .build();
    }
    
    public static ProductResponse from(GetProductResponse useCaseResponse) {
        return ProductResponse.builder()
                .productId(useCaseResponse.getProductId())
                .name(useCaseResponse.getName())
                .description(useCaseResponse.getDescription())
                .type(useCaseResponse.getType())
                .status(useCaseResponse.getStatus())
                .build();
    }
}