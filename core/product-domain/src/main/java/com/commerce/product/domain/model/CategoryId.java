package com.commerce.product.domain.model;

import com.commerce.common.domain.model.ValueObject;
import com.commerce.product.domain.exception.InvalidCategoryIdException;

public record CategoryId(String value) implements ValueObject {
    
    public CategoryId {
        validate(value);
    }

    private static void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCategoryIdException("Category ID cannot be null or empty");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}