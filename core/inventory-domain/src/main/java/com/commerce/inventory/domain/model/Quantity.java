package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidQuantityException;
import com.commerce.product.domain.model.ValueObject;

public record Quantity(int value) implements ValueObject {
    
    public Quantity {
        if (value < 0) {
            throw new InvalidQuantityException("수량은 0 이상이어야 합니다");
        }
    }
    
    public static Quantity of(int value) {
        return new Quantity(value);
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }
    
    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value);
    }
    
    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value >= other.value;
    }
    
    public int getValue() {
        return value;
    }
}