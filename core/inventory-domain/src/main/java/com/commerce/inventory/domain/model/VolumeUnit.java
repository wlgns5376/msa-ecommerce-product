package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidVolumeException;

public enum VolumeUnit {
    CUBIC_CM,
    CUBIC_M,
    LITER,
    MILLILITER;
    
    public static VolumeUnit fromString(String unit) {
        try {
            return VolumeUnit.valueOf(unit.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidVolumeException("유효하지 않은 부피 단위입니다: " + unit);
        }
    }
}