package com.commerce.inventory.domain.exception;

public class InvalidReservationIdException extends InventoryDomainException {
    
    public InvalidReservationIdException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_RESERVATION_ID";
    }
}