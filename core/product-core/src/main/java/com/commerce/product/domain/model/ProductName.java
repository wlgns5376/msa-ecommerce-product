package com.commerce.product.domain.model;

import com.commerce.common.domain.model.ValueObject;
import com.commerce.product.domain.exception.InvalidProductNameException;

public record ProductName(String value) implements ValueObject {
    private static final int MAX_LENGTH = 100;
    
    public ProductName {
        validate(value);
    }
    
    public static ProductName of(String value) {
        return new ProductName(value);
    }

    private static void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidProductNameException("Product name cannot be null or empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new InvalidProductNameException("Product name cannot exceed " + MAX_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}