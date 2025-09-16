package com.commerce.inventory.domain.exception;

public class InvalidSkuCodeException extends InventoryDomainException {
    
    public InvalidSkuCodeException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_SKU_CODE";
    }
}