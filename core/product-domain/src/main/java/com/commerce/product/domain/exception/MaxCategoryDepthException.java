package com.commerce.product.domain.exception;

public class MaxCategoryDepthException extends ProductDomainException {
    public MaxCategoryDepthException(String message) {
        super(message);
    }
}