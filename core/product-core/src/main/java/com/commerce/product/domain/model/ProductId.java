package com.commerce.product.domain.model;

import com.commerce.common.domain.model.ValueObject;
import com.commerce.product.domain.exception.InvalidProductIdException;

import java.util.UUID;

public record ProductId(String value) implements ValueObject {
    
    public ProductId {
        validate(value);
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    private static void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidProductIdException("Product ID cannot be null or empty");
        }

        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidProductIdException("Invalid product ID: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}