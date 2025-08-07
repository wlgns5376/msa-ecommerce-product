package com.commerce.product.domain.exception;

public class CannotDeactivateCategoryException extends ProductDomainException {
    public CannotDeactivateCategoryException(String message) {
        super(message);
    }
}