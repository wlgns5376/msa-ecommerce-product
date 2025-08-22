package com.commerce.inventory.domain.exception;

public class InvalidQuantityException extends InventoryDomainException {
    
    public InvalidQuantityException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_QUANTITY";
    }
}
