package com.commerce.product.common.exception;

/**
 * 도메인 규칙 위반 관련 예외
 */
public abstract class DomainException extends BusinessException {
    
    protected DomainException(String message) {
        super(message);
    }
    
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}