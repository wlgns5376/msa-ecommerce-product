package com.commerce.inventory.domain.exception;

public class InsufficientStockException extends InventoryDomainException {
    
    public InsufficientStockException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INSUFFICIENT_STOCK";
    }
}