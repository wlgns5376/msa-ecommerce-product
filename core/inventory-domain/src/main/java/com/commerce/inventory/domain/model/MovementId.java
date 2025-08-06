package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidMovementIdException;
import com.commerce.product.domain.model.ValueObject;

import java.util.UUID;

public record MovementId(String value) implements ValueObject {
    
    public MovementId {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidMovementIdException("Movement ID는 필수입니다");
        }
    }
    
    public static MovementId generate() {
        return new MovementId(UUID.randomUUID().toString());
    }
}