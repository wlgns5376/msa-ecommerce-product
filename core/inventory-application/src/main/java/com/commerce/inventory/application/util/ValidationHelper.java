package com.commerce.inventory.application.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
}