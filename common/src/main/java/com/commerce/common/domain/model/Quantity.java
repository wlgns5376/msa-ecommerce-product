package com.commerce.common.domain.model;

public record Quantity(int value) implements ValueObject {
    
    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다");
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