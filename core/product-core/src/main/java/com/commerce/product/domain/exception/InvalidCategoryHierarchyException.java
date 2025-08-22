package com.commerce.product.domain.exception;

public class InvalidCategoryHierarchyException extends ProductDomainException {
    public InvalidCategoryHierarchyException(String message) {
        super(message);
    }
}