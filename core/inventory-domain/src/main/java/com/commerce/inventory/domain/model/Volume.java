package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidVolumeException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Volume implements ValueObject {
    
    private final double value;
    private final VolumeUnit unit;
    
    public Volume(double value, VolumeUnit unit) {
        if (value < 0) {
            throw new InvalidVolumeException("부피는 0 이상이어야 합니다");
        }
        if (unit == null) {
            throw new InvalidVolumeException("부피 단위는 필수입니다");
        }
        this.value = value;
        this.unit = unit;
    }
}