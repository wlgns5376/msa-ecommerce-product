package com.commerce.inventory.domain.exception;

public class InvalidStockMovementException extends InventoryDomainException {
    
    public InvalidStockMovementException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_STOCK_MOVEMENT";
    }
}