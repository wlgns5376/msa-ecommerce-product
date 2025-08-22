package com.commerce.product.domain.exception;

public class InvalidCategoryIdException extends ProductDomainException {
    public InvalidCategoryIdException(String message) {
        super(message);
    }
}