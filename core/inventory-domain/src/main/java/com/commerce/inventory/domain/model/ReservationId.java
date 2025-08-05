package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidReservationIdException;
import com.commerce.product.domain.model.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class ReservationId implements ValueObject {
    
    private final String value;
    
    public ReservationId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidReservationIdException("Reservation ID는 필수입니다");
        }
        this.value = value;
    }
    
    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID().toString());
    }
}