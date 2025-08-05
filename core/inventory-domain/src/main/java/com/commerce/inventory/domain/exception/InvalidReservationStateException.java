package com.commerce.inventory.domain.exception;

public class InvalidReservationStateException extends InventoryDomainException {
    
    public InvalidReservationStateException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_RESERVATION_STATE";
    }
}