package com.commerce.product.domain.exception;

import com.commerce.common.exception.DomainException;

public class ProductDomainException extends DomainException {
    private static final String ERROR_CODE_PREFIX = "PRODUCT_";
    
    public ProductDomainException(String message) {
        super(message);
    }

    public ProductDomainException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE_PREFIX + this.getClass().getSimpleName().replace("Exception", "").toUpperCase();
    }
}