package com.commerce.common.domain.model;

/**
 * 수량을 나타내는 값 객체
 */
public record Quantity(int value) implements ValueObject {
    
    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
    
    public static Quantity of(int value) {
        return new Quantity(value);
    }
    
    public static Quantity zero() {
        return new Quantity(0);
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }
    
    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value);
    }
    
    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }
    
    public boolean isGreaterThanOrEqualTo(Quantity other) {
        return this.value >= other.value;
    }
    
    public boolean isLessThan(Quantity other) {
        return this.value < other.value;
    }
    
    public boolean isLessThanOrEqualTo(Quantity other) {
        return this.value <= other.value;
    }
    
    public boolean isZero() {
        return this.value == 0;
    }
}