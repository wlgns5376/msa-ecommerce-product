package com.commerce.product.domain.exception;

public class MaxCategoryLimitException extends ProductDomainException {
    public MaxCategoryLimitException(String message) {
        super(message);
    }
}