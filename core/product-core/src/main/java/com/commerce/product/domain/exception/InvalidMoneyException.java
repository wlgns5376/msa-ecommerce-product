package com.commerce.product.domain.exception;

public class InvalidMoneyException extends ProductDomainException {
    public InvalidMoneyException(String message) {
        super(message);
    }
}