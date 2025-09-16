package com.commerce.product.domain.exception;

public class InvalidCategoryNameException extends ProductDomainException {
    public InvalidCategoryNameException(String message) {
        super(message);
    }
}