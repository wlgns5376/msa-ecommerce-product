package com.commerce.inventory.domain.application.usecase.validation;

import com.commerce.inventory.domain.exception.InvalidReservationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 요청 객체의 유효성을 검증하는 범용 검증기
 * 
 * <p>이 클래스는 빌더 패턴을 사용하여 유창한 API로 검증 규칙을 정의할 수 있습니다.
 * 검증은 execute() 메서드 호출 시 수행되며, 첫 번째 실패한 규칙에서 예외를 발생시킵니다.
 * 
 * @param <T> 검증 대상 객체의 타입
 */
public class RequestValidator<T> {
    
    private final T target;
    private final List<ValidationRule<T>> rules = new ArrayList<>();
    
    private RequestValidator(T target) {
        this.target = target;
    }
    
    /**
     * 검증기 인스턴스를 생성합니다.
     * 
     * @param target 검증할 대상 객체
     * @return 새로운 RequestValidator 인스턴스
     */
    public static <T> RequestValidator<T> of(T target) {
        return new RequestValidator<>(target);
    }
    
    /**
     * 커스텀 검증 규칙을 추가합니다.
     * 
     * @param condition 검증 조건
     * @param errorMessage 검증 실패 시 표시할 에러 메시지
     * @return 체이닝을 위한 현재 인스턴스
     */
    public RequestValidator<T> validate(Predicate<T> condition, String errorMessage) {
        Objects.requireNonNull(condition, "검증 조건은 null일 수 없습니다");
        Objects.requireNonNull(errorMessage, "에러 메시지는 null일 수 없습니다");
        rules.add(new ValidationRule<>(condition, errorMessage));
        return this;
    }
    
    /**
     * 특정 필드에 대한 검증 규칙을 추가합니다.
     * 
     * @param fieldExtractor 필드 값을 추출하는 함수
     * @param condition 필드 값에 대한 검증 조건
     * @param errorMessage 검증 실패 시 표시할 에러 메시지
     * @return 체이닝을 위한 현재 인스턴스
     */
    public <U> RequestValidator<T> validateField(
            Function<T, U> fieldExtractor,
            Predicate<U> condition,
            String errorMessage
    ) {
        Objects.requireNonNull(fieldExtractor, "필드 추출 함수는 null일 수 없습니다");
        Objects.requireNonNull(condition, "검증 조건은 null일 수 없습니다");
        Objects.requireNonNull(errorMessage, "에러 메시지는 null일 수 없습니다");
        
        rules.add(new ValidationRule<>(
                t -> {
                    if (t == null) {
                        return false;
                    }
                    try {
                        U value = fieldExtractor.apply(t);
                        return condition.test(value);
                    } catch (NullPointerException e) {
                        return false;
                    }
                },
                errorMessage
        ));
        return this;
    }
    
    /**
     * 필드가 null이 아님을 검증합니다.
     */
    public RequestValidator<T> notNull(Function<T, ?> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                Objects::nonNull,
                fieldName + "은(는) 필수입니다"
        );
    }
    
    /**
     * 문자열 필드가 null이 아니고 비어있지 않음을 검증합니다.
     */
    public RequestValidator<T> notEmpty(Function<T, String> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                value -> value != null && !value.isBlank(),
                fieldName + "은(는) 필수입니다"
        );
    }
    
    /**
     * 리스트 필드가 null이 아니고 비어있지 않음을 검증합니다.
     */
    public RequestValidator<T> notEmptyList(Function<T, List<?>> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                list -> list != null && !list.isEmpty(),
                fieldName + "이(가) 비어있습니다"
        );
    }
    
    /**
     * 정수 필드가 양수임을 검증합니다.
     */
    public RequestValidator<T> positive(Function<T, Integer> fieldExtractor, String fieldName) {
        return validateField(
                fieldExtractor,
                value -> value != null && value > 0,
                fieldName + "은(는) 0보다 커야 합니다"
        );
    }
    
    /**
     * 리스트의 각 항목에 대해 검증을 수행합니다.
     * 
     * <p>이 메서드는 리스트의 각 항목에 대해 주어진 검증 규칙을 적용합니다.
     * 검증 실패 시 어떤 항목에서 실패했는지 명확하게 표시합니다.
     * 
     * @param listExtractor 리스트를 추출하는 함수
     * @param itemValidator 각 항목에 적용할 검증 규칙을 정의하는 Consumer
     * @return 체이닝을 위한 현재 인스턴스
     */
    public <U> RequestValidator<T> validateEach(
            Function<T, List<U>> listExtractor,
            Consumer<RequestValidator<U>> itemValidator
    ) {
        Objects.requireNonNull(listExtractor, "리스트 추출 함수는 null일 수 없습니다");
        Objects.requireNonNull(itemValidator, "항목 검증기는 null일 수 없습니다");
        
        // 리스트 검증을 위한 규칙 추가
        validate(t -> {
            if (t == null) {
                return true;
            }
            
            List<U> items = extractList(t, listExtractor);
            if (items == null) {
                return true;
            }
            
            // 각 항목에 대해 검증 수행
            validateListItems(items, itemValidator);
            return true;
        }, "리스트 항목 검증");
        
        return this;
    }
    
    /**
     * 안전하게 리스트를 추출합니다.
     */
    private <U> List<U> extractList(T target, Function<T, List<U>> listExtractor) {
        try {
            return listExtractor.apply(target);
        } catch (NullPointerException e) {
            return null;
        }
    }
    
    /**
     * 리스트의 각 항목을 검증합니다.
     */
    private <U> void validateListItems(List<U> items, Consumer<RequestValidator<U>> itemValidator) {
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
    
    /**
     * 정의된 모든 검증 규칙을 실행합니다.
     * 
     * @throws InvalidReservationException 검증 실패 시
     */
    public void execute() {
        for (ValidationRule<T> rule : rules) {
            if (!rule.condition.test(target)) {
                throw new InvalidReservationException(rule.errorMessage);
            }
        }
    }
    
    /**
     * 검증 규칙을 나타내는 내부 클래스
     */
    private static class ValidationRule<T> {
        private final Predicate<T> condition;
        private final String errorMessage;
        
        ValidationRule(Predicate<T> condition, String errorMessage) {
            this.condition = condition;
            this.errorMessage = errorMessage;
        }
    }
}