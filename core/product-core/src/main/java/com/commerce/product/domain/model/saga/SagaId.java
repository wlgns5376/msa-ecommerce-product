package com.commerce.product.domain.model.saga;

import com.commerce.common.domain.model.ValueObject;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
public class SagaId implements ValueObject {
    private final String value;
    
    private SagaId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SagaId cannot be null or empty");
        }
        this.value = value;
    }
    
    public static SagaId generate() {
        return new SagaId(UUID.randomUUID().toString());
    }
    
    public static SagaId of(String value) {
        return new SagaId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}