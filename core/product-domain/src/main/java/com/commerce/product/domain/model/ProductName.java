package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductNameException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ProductName implements ValueObject {
    private static final int MAX_LENGTH = 100;
    private final String value;

    public ProductName(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
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