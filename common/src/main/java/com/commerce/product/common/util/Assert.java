package com.commerce.boilerplate.common.util;

import java.util.Collection;

/**
 * 도메인 검증을 위한 유틸리티 클래스
 */
public final class Assert {
    
    private Assert() {
        // 유틸리티 클래스
    }
    
    /**
     * 값이 null이 아닌지 검증합니다.
     */
    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 문자열이 비어있지 않은지 검증합니다.
     */
    public static void hasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 조건이 참인지 검증합니다.
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 컬렉션이 비어있지 않은지 검증합니다.
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 값이 양수인지 검증합니다.
     */
    public static void isPositive(Number value, String message) {
        notNull(value, message);
        if (value.doubleValue() <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}