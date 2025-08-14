package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidWeightException;

public enum WeightUnit {
    GRAM,
    KILOGRAM,
    MILLIGRAM;
    
    public static WeightUnit fromString(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            throw new InvalidWeightException("무게 단위는 null이거나 비어있을 수 없습니다.");
        }
        try {
            return WeightUnit.valueOf(unit.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidWeightException("유효하지 않은 무게 단위입니다: " + unit, e);
        }
    }
}