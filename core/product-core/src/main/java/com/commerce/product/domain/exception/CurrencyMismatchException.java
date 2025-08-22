package com.commerce.product.domain.exception;

public class CurrencyMismatchException extends ProductDomainException {
    public CurrencyMismatchException() {
        super("Cannot operate on different currencies");
    }

    public CurrencyMismatchException(String message) {
        super(message);
    }
}