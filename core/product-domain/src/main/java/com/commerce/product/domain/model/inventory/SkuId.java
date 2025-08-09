package com.commerce.product.domain.model.inventory;

import com.commerce.common.domain.model.ValueObject;

public record SkuId(String value) implements ValueObject {
    
    public SkuId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
    }
    
    public static SkuId of(String value) {
        return new SkuId(value);
    }
}