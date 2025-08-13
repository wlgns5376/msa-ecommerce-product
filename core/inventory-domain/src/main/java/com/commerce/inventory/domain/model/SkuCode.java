package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import com.commerce.common.domain.model.ValueObject;

public record SkuCode(String value) implements ValueObject {
    
    public SkuCode {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidSkuCodeException("SKU 코드는 필수입니다");
        }
        
        if (!value.matches("^[A-Za-z0-9\\-_]+$")) {
            throw new InvalidSkuCodeException("SKU 코드는 영문자, 숫자, 하이픈, 언더스코어만 허용됩니다: " + value);
        }
    }
    
    public static SkuCode of(String value) {
        return new SkuCode(value);
    }
    
    public String getValue() {
        return value;
    }
}