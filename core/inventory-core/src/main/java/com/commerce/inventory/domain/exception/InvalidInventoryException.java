package com.commerce.inventory.domain.exception;

public class InvalidInventoryException extends InventoryDomainException {
    
    public InvalidInventoryException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_INVENTORY";
    }
}