package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidVolumeException;

public enum VolumeUnit {
    CUBIC_CM,
    CUBIC_M,
    LITER,
    MILLILITER;
    
    public static VolumeUnit fromString(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            throw new InvalidVolumeException("부피 단위는 null이거나 비어있을 수 없습니다.");
        }
        try {
            return VolumeUnit.valueOf(unit.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidVolumeException("유효하지 않은 부피 단위입니다: " + unit, e);
        }
    }
}