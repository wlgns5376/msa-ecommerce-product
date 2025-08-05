package com.commerce.inventory.domain.exception;

public class InvalidSkuException extends InventoryDomainException {
    
    public InvalidSkuException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_SKU";
    }
}