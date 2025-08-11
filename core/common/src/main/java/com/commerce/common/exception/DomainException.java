package com.commerce.common.exception;

/**
 * 도메인 예외의 기본 클래스
 */
public abstract class DomainException extends RuntimeException {
    
    public DomainException(String message) {
        super(message);
    }
    
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public abstract String getErrorCode();
}