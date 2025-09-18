package com.commerce.inventory.application.usecase;

import lombok.Getter;

@Getter
public class GetSkuByIdQuery {
    
    private final String skuId;
    
    private GetSkuByIdQuery(String skuId) {
        validateQuery(skuId);
        this.skuId = skuId;
    }
    
    public static GetSkuByIdQuery of(String skuId) {
        return new GetSkuByIdQuery(skuId);
    }
    
    private void validateQuery(String skuId) {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
    }
}