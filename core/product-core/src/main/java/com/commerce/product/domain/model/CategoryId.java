package com.commerce.product.domain.model;

import com.commerce.common.domain.model.ValueObject;
import com.commerce.product.domain.exception.InvalidCategoryIdException;

import java.util.UUID;

public record CategoryId(String value) implements ValueObject {
    
    public CategoryId {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCategoryIdException("Category ID cannot be null or empty");
        }
    }
    
    public static CategoryId generate() {
        return new CategoryId(UUID.randomUUID().toString());
    }
    
    public static CategoryId of(String value) {
        return new CategoryId(value);
    }
}