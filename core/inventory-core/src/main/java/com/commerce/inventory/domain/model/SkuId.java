package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.common.domain.model.ValueObject;

import java.util.UUID;

public record SkuId(String value) implements ValueObject {
    
    public SkuId {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidSkuIdException("SKU ID는 필수입니다");
        }
    }
    
    public static SkuId generate() {
        return new SkuId(UUID.randomUUID().toString());
    }
    
    public static SkuId of(String value) {
        return new SkuId(value);
    }
}