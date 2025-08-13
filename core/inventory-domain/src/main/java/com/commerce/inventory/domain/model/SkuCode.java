package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import com.commerce.common.domain.model.ValueObject;

public record SkuCode(String value) implements ValueObject {
    
    public SkuCode {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidSkuCodeException("SKU 코드는 필수입니다");
        }
    }
    
    public static SkuCode of(String value) {
        return new SkuCode(value);
    }
}