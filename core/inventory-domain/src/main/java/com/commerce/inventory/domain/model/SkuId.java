package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class SkuId implements ValueObject {
    
    private final String value;
    
    public SkuId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidSkuIdException("SKU ID는 필수입니다");
        }
        this.value = value;
    }
    
    public static SkuId generate() {
        return new SkuId(UUID.randomUUID().toString());
    }
}