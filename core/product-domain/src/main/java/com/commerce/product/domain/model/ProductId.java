package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductIdException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class ProductId implements ValueObject {
    private final String value;

    public ProductId(String value) {
        validate(value);
        this.value = value;
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    private void validate(String value) {
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