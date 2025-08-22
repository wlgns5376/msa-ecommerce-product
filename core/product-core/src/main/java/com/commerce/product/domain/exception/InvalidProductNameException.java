package com.commerce.product.domain.exception;

public class InvalidProductNameException extends ProductDomainException {
    public InvalidProductNameException(String message) {
        super(message);
    }
}