package com.commerce.boilerplate.common.exception;

/**
 * 비즈니스 로직 관련 예외의 기본 클래스
 */
public abstract class BusinessException extends RuntimeException {
    
    protected BusinessException(String message) {
        super(message);
    }
    
    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 에러 코드를 반환합니다.
     */
    public abstract String getErrorCode();
}