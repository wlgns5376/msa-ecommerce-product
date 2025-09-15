package com.commerce.inventory.domain.exception;

public class InvalidWeightException extends InventoryDomainException {
    
    public InvalidWeightException(String message) {
        super(message);
    }
    
    public InvalidWeightException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_WEIGHT";
    }
}