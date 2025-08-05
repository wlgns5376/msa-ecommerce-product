package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class SkuCode implements ValueObject {
    
    private final String value;
    
    public SkuCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidSkuCodeException("SKU 코드는 필수입니다");
        }
        this.value = value;
    }
}