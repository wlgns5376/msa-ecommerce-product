package com.commerce.product.domain.model.inventory;

import com.commerce.product.domain.model.ValueObject;

public record Quantity(int value) implements ValueObject {
    
    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
    
    public static Quantity of(int value) {
        return new Quantity(value);
    }
    
    public int getValue() {
        return value;
    }
}