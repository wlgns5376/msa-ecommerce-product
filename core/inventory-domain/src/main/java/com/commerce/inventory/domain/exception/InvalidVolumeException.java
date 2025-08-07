package com.commerce.inventory.domain.exception;

public class InvalidVolumeException extends InventoryDomainException {
    
    public InvalidVolumeException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_VOLUME";
    }
}