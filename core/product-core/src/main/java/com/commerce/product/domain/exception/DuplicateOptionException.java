package com.commerce.product.domain.exception;

public class DuplicateOptionException extends ProductDomainException {
    
    public DuplicateOptionException(String message) {
        super(message);
    }
    
    public DuplicateOptionException(String message, Throwable cause) {
        super(message, cause);
    }
}