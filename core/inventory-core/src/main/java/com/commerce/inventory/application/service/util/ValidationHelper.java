package com.commerce.inventory.application.util;

import com.commerce.inventory.domain.exception.InvalidReservationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ValidationHelper {
    
    private ValidationHelper() {
        // 유틸리티 클래스는 인스턴스화할 수 없습니다.
    }
    
    public static <T> void validate(Validator validator, T objectToValidate) {
        Set<ConstraintViolation<T>> violations = validator.validate(objectToValidate);
        if (!violations.isEmpty()) {
            String combinedMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(combinedMessage);
        }
    }
    
    public static void validateNotNull(Object object, String message) {
        if (object == null) {
            throw new InvalidReservationException(message);
        }
    }
    
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidReservationException(fieldName + "은(는) 필수입니다");
        }
    }
    
    public static void validateNotEmptyList(List<?> list, String fieldName) {
        if (list == null || list.isEmpty()) {
            throw new InvalidReservationException(fieldName + "이(가) 비어있습니다");
        }
    }
    
    public static void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidReservationException(fieldName + "은(는) 0보다 커야 합니다");
        }
    }
}