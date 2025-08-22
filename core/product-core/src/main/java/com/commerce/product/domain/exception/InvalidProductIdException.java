package com.commerce.product.domain.exception;

public class InvalidProductIdException extends ProductDomainException {
    public InvalidProductIdException(String message) {
        super(message);
    }
}