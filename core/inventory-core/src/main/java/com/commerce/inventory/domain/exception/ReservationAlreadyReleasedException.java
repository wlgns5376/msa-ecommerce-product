package com.commerce.inventory.domain.exception;

public class ReservationAlreadyReleasedException extends InventoryDomainException {
    public ReservationAlreadyReleasedException(String message) {
        super(message);
    }

    public ReservationAlreadyReleasedException(String message, Throwable cause) {
        super(message, cause);
    }
}