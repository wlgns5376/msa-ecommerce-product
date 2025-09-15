package com.commerce.inventory.domain.exception;

public class InvalidSkuIdException extends InventoryDomainException {
    
    public InvalidSkuIdException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_SKU_ID";
    }
}