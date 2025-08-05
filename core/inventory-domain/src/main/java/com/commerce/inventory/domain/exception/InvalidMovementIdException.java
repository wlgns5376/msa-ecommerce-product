package com.commerce.inventory.domain.exception;

public class InvalidMovementIdException extends InventoryDomainException {
    
    public InvalidMovementIdException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_MOVEMENT_ID";
    }
}