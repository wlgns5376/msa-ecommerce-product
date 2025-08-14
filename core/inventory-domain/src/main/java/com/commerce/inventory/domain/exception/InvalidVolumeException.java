package com.commerce.inventory.domain.exception;

public class InvalidVolumeException extends InventoryDomainException {
    
    public InvalidVolumeException(String message) {
        super(message);
    }
    
    public InvalidVolumeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_VOLUME";
    }
}