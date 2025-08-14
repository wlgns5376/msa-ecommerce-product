package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidWeightException;

public enum WeightUnit {
    GRAM,
    KILOGRAM,
    MILLIGRAM;
    
    public static WeightUnit fromString(String unit) {
        try {
            return WeightUnit.valueOf(unit.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidWeightException("유효하지 않은 무게 단위입니다: " + unit);
        }
    }
}