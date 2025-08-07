package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidCategoryNameException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CategoryName implements ValueObject {
    private static final int MAX_LENGTH = 50;
    private final String value;

    public CategoryName(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
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