package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidMovementIdException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class MovementId implements ValueObject {
    
    private final String value;
    
    public MovementId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidMovementIdException("Movement ID는 필수입니다");
        }
        this.value = value;
    }
    
    public static MovementId generate() {
        return new MovementId(UUID.randomUUID().toString());
    }
}