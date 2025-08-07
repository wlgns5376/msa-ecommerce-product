package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidReservationIdException;
import com.commerce.product.domain.model.ValueObject;

import java.util.UUID;

public record ReservationId(String value) implements ValueObject {
    
    public ReservationId {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidReservationIdException("Reservation ID는 필수입니다");
        }
    }
    
    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID().toString());
    }
}