package com.commerce.inventory.domain.exception;

public class SkuNotFoundException extends InventoryDomainException {
    
    public SkuNotFoundException(String message) {
        super(message);
    }
    
    public SkuNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}