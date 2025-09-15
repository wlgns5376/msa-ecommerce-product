package com.commerce.inventory.domain.exception;

public class InvalidReservationException extends InventoryDomainException {
    
    public InvalidReservationException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_RESERVATION";
    }
}