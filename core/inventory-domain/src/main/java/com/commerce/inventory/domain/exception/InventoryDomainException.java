package com.commerce.inventory.domain.exception;

import com.commerce.common.exception.DomainException;

public abstract class InventoryDomainException extends DomainException {
    private static final String ERROR_CODE_PREFIX = "INVENTORY_";
    
    protected InventoryDomainException(String message) {
        super(message);
    }
    
    protected InventoryDomainException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE_PREFIX + this.getClass().getSimpleName().replace("Exception", "").toUpperCase();
    }
}