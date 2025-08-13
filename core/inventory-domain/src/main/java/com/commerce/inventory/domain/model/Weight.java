package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidWeightException;
import com.commerce.common.domain.model.ValueObject;

public record Weight(double value, WeightUnit unit) implements ValueObject {
    
    public Weight {
        if (value < 0) {
            throw new InvalidWeightException("무게는 0 이상이어야 합니다");
        }
        if (unit == null) {
            throw new InvalidWeightException("무게 단위는 필수입니다");
        }
    }
    
    public static Weight of(double value, WeightUnit unit) {
        return new Weight(value, unit);
    }
}