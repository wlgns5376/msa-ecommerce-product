package com.commerce.product.domain.model;

import com.commerce.common.domain.model.ValueObject;
import com.commerce.product.domain.exception.InvalidCategoryNameException;

public record CategoryName(String value) implements ValueObject {
    private static final int MAX_LENGTH = 50;
    
    public CategoryName {
        validate(value);
    }

    private static void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCategoryNameException("Category name cannot be null or empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new InvalidCategoryNameException("Category name cannot exceed " + MAX_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}