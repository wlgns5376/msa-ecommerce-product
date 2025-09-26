package com.commerce.product.api.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductOptionResponse {
    
    private String productId;
    private String optionId;
    private String optionName;
    
    public static AddProductOptionResponse from(com.commerce.product.application.usecase.AddProductOptionResponse response) {
        return AddProductOptionResponse.builder()
                .productId(response.getProductId())
                .optionId(response.getOptionId())
                .optionName(response.getOptionName())
                .build();
    }
}