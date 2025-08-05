package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidQuantityException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Quantity implements ValueObject {
    
    private final int value;
    
    public Quantity(int value) {
        if (value < 0) {
            throw new InvalidQuantityException("수량은 0 이상이어야 합니다");
        }
        this.value = value;
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
}