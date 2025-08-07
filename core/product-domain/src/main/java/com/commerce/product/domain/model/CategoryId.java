package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CategoryId implements ValueObject {
    private final String value;

    public CategoryId(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCategoryIdException("Category ID cannot be null or empty");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}