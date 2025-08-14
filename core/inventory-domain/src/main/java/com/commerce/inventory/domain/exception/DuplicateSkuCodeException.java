package com.commerce.inventory.domain.exception;

public class DuplicateSkuCodeException extends InventoryDomainException {
    
    public DuplicateSkuCodeException(String message) {
        super(message);
    }
    
    public DuplicateSkuCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}