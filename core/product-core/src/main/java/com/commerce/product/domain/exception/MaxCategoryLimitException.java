package com.commerce.product.domain.exception;

public class MaxCategoryLimitException extends ProductDomainException {
    
    public MaxCategoryLimitException(String message) {
        super(message);
    }
    
    public MaxCategoryLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}