package com.commerce.inventory.domain.application.usecase.validation;

import com.commerce.inventory.domain.exception.InvalidReservationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class RequestValidator<T> {
    
    private final T target;
    private final List<ValidationRule<T>> rules = new ArrayList<>();
    
    private RequestValidator(T target) {
        this.target = target;
    }
    
    public static <T> RequestValidator<T> of(T target) {
        return new RequestValidator<>(target);
    }
    
    public RequestValidator<T> validate(Predicate<T> condition, String errorMessage) {
        rules.add(new ValidationRule<>(condition, errorMessage));
        return this;
    }
    
    public <U> RequestValidator<T> validateField(
            Function<T, U> fieldExtractor,
            Predicate<U> condition,
            String errorMessage
    ) {
        rules.add(new ValidationRule<>(
                t -> condition.test(fieldExtractor.apply(t)),
                errorMessage
        ));
        return this;
    }
    
    public RequestValidator<T> notNull(Function<T, ?> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                value -> value != null,
                fieldName + "은(는) 필수입니다"
        );
    }
    
    public RequestValidator<T> notEmpty(Function<T, String> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                value -> value != null && !value.trim().isEmpty(),
                fieldName + "은(는) 필수입니다"
        );
    }
    
    public RequestValidator<T> notEmptyList(Function<T, List<?>> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                list -> list != null && !list.isEmpty(),
                fieldName + "이(가) 비어있습니다"
        );
    }
    
    public RequestValidator<T> positive(Function<T, Integer> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                value -> value != null && value > 0,
                fieldName + "은(는) 0보다 커야 합니다"
        );
    }
    
    public <U> RequestValidator<T> validateEach(
            Function<T, List<U>> listExtractor,
            Consumer<RequestValidator<U>> itemValidator
    ) {
        List<U> items = listExtractor.apply(target);
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                U item = items.get(i);
                try {
                    RequestValidator<U> validator = RequestValidator.of(item);
                    itemValidator.accept(validator);
                    validator.execute();
                } catch (InvalidReservationException e) {
                    throw new InvalidReservationException(
                            String.format("항목 %d번째 검증 실패: %s", i + 1, e.getMessage())
                    );
                }
            }
        }
        return this;
    }
    
    public void execute() {
        for (ValidationRule<T> rule : rules) {
            if (!rule.condition.test(target)) {
                throw new InvalidReservationException(rule.errorMessage);
            }
        }
    }
    
    private static class ValidationRule<T> {
        private final Predicate<T> condition;
        private final String errorMessage;
        
        public ValidationRule(Predicate<T> condition, String errorMessage) {
            this.condition = condition;
            this.errorMessage = errorMessage;
        }
    }
}