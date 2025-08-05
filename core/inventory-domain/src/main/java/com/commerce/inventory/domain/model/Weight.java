package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidWeightException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Weight implements ValueObject {
    
    private final double value;
    private final WeightUnit unit;
    
    public Weight(double value, WeightUnit unit) {
        if (value < 0) {
            throw new InvalidWeightException("무게는 0 이상이어야 합니다");
        }
        if (unit == null) {
            throw new InvalidWeightException("무게 단위는 필수입니다");
        }
        this.value = value;
        this.unit = unit;
    }
}