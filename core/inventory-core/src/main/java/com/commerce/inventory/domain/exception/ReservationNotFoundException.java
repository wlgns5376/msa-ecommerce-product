package com.commerce.inventory.domain.exception;

public class ReservationNotFoundException extends InventoryDomainException {
    public ReservationNotFoundException(String message) {
        super(message);
    }

    public ReservationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}