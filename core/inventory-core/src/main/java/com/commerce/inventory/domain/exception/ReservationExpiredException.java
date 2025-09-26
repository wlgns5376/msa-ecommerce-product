package com.commerce.inventory.domain.exception;

public class ReservationExpiredException extends InventoryDomainException {
    public ReservationExpiredException(String message) {
        super(message);
    }

    public ReservationExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}