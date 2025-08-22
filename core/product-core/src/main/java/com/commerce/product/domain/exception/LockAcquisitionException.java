package com.commerce.product.domain.exception;

public class LockAcquisitionException extends ProductDomainException {
    
    public LockAcquisitionException(String message) {
        super(message);
    }
    
    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}