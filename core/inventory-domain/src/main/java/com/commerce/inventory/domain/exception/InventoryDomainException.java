package com.commerce.inventory.domain.exception;

import com.commerce.product.common.exception.DomainException;

public abstract class InventoryDomainException extends DomainException {
    
    protected InventoryDomainException(String message) {
        super(message);
    }
    
    protected InventoryDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}