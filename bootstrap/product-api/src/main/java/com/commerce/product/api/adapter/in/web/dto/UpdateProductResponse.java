package com.commerce.product.api.adapter.in.web.dto;

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
    private final Long version;
    
    public static UpdateProductResponse from(com.commerce.product.application.usecase.UpdateProductResponse useCaseResponse) {
        return UpdateProductResponse.builder()
                .productId(useCaseResponse.getProductId())
                .name(useCaseResponse.getName())
                .description(useCaseResponse.getDescription())
                .type(useCaseResponse.getType())
                .status(useCaseResponse.getStatus())
                .version(useCaseResponse.getVersion())
                .build();
    }
}