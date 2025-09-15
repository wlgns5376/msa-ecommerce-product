package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidVolumeException;
import com.commerce.common.domain.model.ValueObject;

public record Volume(double value, VolumeUnit unit) implements ValueObject {
    
    public Volume {
        if (value <= 0) {
            throw new InvalidVolumeException("부피는 0보다 커야 합니다");
        }
        if (unit == null) {
            throw new InvalidVolumeException("부피 단위는 필수입니다");
        }
    }
    
    public static Volume of(double value, VolumeUnit unit) {
        return new Volume(value, unit);
    }
    
}