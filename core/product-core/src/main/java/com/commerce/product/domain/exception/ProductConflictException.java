package com.commerce.product.domain.exception;

public class ProductConflictException extends ProductDomainException {
    
    public ProductConflictException(String message) {
        super(message);
    }
    
    public ProductConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}