package com.commerce.product.domain.exception;

public class InvalidOptionException extends ProductDomainException {
    
    public InvalidOptionException(String message) {
        super(message);
    }
    
    public InvalidOptionException(String message, Throwable cause) {
        super(message, cause);
    }
}