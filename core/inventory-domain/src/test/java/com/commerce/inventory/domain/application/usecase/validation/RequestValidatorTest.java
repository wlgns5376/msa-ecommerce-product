package com.commerce.inventory.domain.application.usecase.validation;

import com.commerce.inventory.domain.exception.InvalidReservationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RequestValidatorTest {
    
    @Test
    @DisplayName("null 대상에 대한 검증을 수행할 수 있다")
    void shouldValidateNullTarget() {
        // given
        TestRequest request = null;
        
        // when & then
        assertThatThrownBy(() -> 
            RequestValidator.of(request)
                .validate(r -> r != null, "요청이 null입니다")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("요청이 null입니다");
    }
    
    @Test
    @DisplayName("단일 조건 검증이 성공한다")
    void shouldPassSingleValidation() {
        // given
        TestRequest request = new TestRequest("test", 10, Arrays.asList("item1"));
        
        // when & then
        assertThatNoException().isThrownBy(() ->
            RequestValidator.of(request)
                .validate(r -> r.name != null, "이름이 null입니다")
                .execute()
        );
    }
    
    @Test
    @DisplayName("단일 조건 검증이 실패한다")
    void shouldFailSingleValidation() {
        // given
        TestRequest request = new TestRequest("test", -1, Arrays.asList("item1"));
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .validate(r -> r.value > 0, "값은 양수여야 합니다")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("값은 양수여야 합니다");
    }
    
    @Test
    @DisplayName("여러 검증 조건을 체이닝할 수 있다")
    void shouldChainMultipleValidations() {
        // given
        TestRequest request = new TestRequest("test", 10, Arrays.asList("item1"));
        
        // when & then
        assertThatNoException().isThrownBy(() ->
            RequestValidator.of(request)
                .notEmpty(TestRequest::getName, "이름")
                .positive(TestRequest::getValue, "값")
                .notEmptyList(TestRequest::getItems, "항목 목록")
                .execute()
        );
    }
    
    @Test
    @DisplayName("첫 번째 실패한 검증에서 즉시 예외를 던진다")
    void shouldFailAtFirstFailedValidation() {
        // given
        TestRequest request = new TestRequest("", 10, Arrays.asList("item1"));
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .notEmpty(TestRequest::getName, "이름")
                .positive(TestRequest::getValue, "값")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("이름은(는) 필수입니다");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("notEmpty는 null, 빈 문자열, 공백 문자열을 거부한다")
    void shouldRejectEmptyStrings(String name) {
        // given
        TestRequest request = new TestRequest(name, 10, Arrays.asList("item1"));
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .notEmpty(TestRequest::getName, "이름")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("이름은(는) 필수입니다");
    }
    
    @Test
    @DisplayName("notEmptyList는 null 리스트를 거부한다")
    void shouldRejectNullList() {
        // given
        TestRequest request = new TestRequest("test", 10, null);
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .notEmptyList(TestRequest::getItems, "항목 목록")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("항목 목록이(가) 비어있습니다");
    }
    
    @Test
    @DisplayName("notEmptyList는 빈 리스트를 거부한다")
    void shouldRejectEmptyList() {
        // given
        TestRequest request = new TestRequest("test", 10, Collections.emptyList());
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .notEmptyList(TestRequest::getItems, "항목 목록")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("항목 목록이(가) 비어있습니다");
    }
    
    @Test
    @DisplayName("positive는 null 값을 거부한다")
    void shouldRejectNullForPositive() {
        // given
        TestRequest request = new TestRequest("test", null, Arrays.asList("item1"));
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .positive(TestRequest::getValue, "값")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("값은(는) 0보다 커야 합니다");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("positive는 0과 음수를 거부한다")
    void shouldRejectNonPositiveValues(int value) {
        // given
        TestRequest request = new TestRequest("test", value, Arrays.asList("item1"));
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .positive(TestRequest::getValue, "값")
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("값은(는) 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("validateEach로 리스트의 각 항목을 검증할 수 있다")
    void shouldValidateEachItemInList() {
        // given
        TestRequest request = new TestRequest("test", 10, 
            Arrays.asList("item1", "item2", "item3"));
        
        // when & then
        assertThatNoException().isThrownBy(() ->
            RequestValidator.of(request)
                .validateEach(TestRequest::getItems, itemValidator -> 
                    itemValidator.validate(item -> item != null && !item.isEmpty(), 
                        "항목이 비어있습니다")
                )
                .execute()
        );
    }
    
    @Test
    @DisplayName("validateEach에서 실패한 항목의 인덱스를 포함한 에러 메시지를 반환한다")
    void shouldIncludeIndexInValidateEachError() {
        // given
        TestRequest request = new TestRequest("test", 10, 
            Arrays.asList("item1", "", "item3"));
        
        // when & then
        assertThatThrownBy(() ->
            RequestValidator.of(request)
                .validateEach(TestRequest::getItems, itemValidator -> 
                    itemValidator.validate(item -> item != null && !item.isEmpty(), 
                        "항목이 비어있습니다")
                )
                .execute()
        )
        .isInstanceOf(InvalidReservationException.class)
        .hasMessage("항목 2번째 검증 실패: 항목이 비어있습니다");
    }
    
    @Test
    @DisplayName("validateEach에서 null 리스트는 검증을 건너뛴다")
    void shouldSkipValidationForNullListInValidateEach() {
        // given
        TestRequest request = new TestRequest("test", 10, null);
        
        // when & then
        assertThatNoException().isThrownBy(() ->
            RequestValidator.of(request)
                .validateEach(TestRequest::getItems, itemValidator -> 
                    itemValidator.validate(item -> false, "항상 실패")
                )
                .execute()
        );
    }
    
    @Test
    @DisplayName("복잡한 중첩 검증을 수행할 수 있다")
    void shouldPerformComplexNestedValidation() {
        // given
        NestedRequest request = new NestedRequest(
            "parent",
            Arrays.asList(
                new TestRequest("child1", 10, Arrays.asList("item1")),
                new TestRequest("child2", 20, Arrays.asList("item2", "item3"))
            )
        );
        
        // when & then
        assertThatNoException().isThrownBy(() ->
            RequestValidator.of(request)
                .notEmpty(NestedRequest::getName, "부모 이름")
                .notEmptyList(NestedRequest::getChildren, "자식 목록")
                .validateEach(NestedRequest::getChildren, childValidator ->
                    childValidator
                        .notEmpty(TestRequest::getName, "자식 이름")
                        .positive(TestRequest::getValue, "자식 값")
                        .notEmptyList(TestRequest::getItems, "자식 항목")
                )
                .execute()
        );
    }
    
    @Test
    @DisplayName("validateField로 커스텀 필드 검증을 수행할 수 있다")
    void shouldValidateCustomField() {
        // given
        TestRequest request = new TestRequest("test123", 10, Arrays.asList("item1"));
        
        // when & then
        assertThatNoException().isThrownBy(() ->
            RequestValidator.of(request)
                .validateField(
                    TestRequest::getName,
                    name -> name.length() >= 5,
                    "이름은 최소 5자 이상이어야 합니다"
                )
                .execute()
        );
    }
    
    // Test helper classes
    private static class TestRequest {
        private final String name;
        private final Integer value;
        private final List<String> items;
        
        public TestRequest(String name, Integer value, List<String> items) {
            this.name = name;
            this.value = value;
            this.items = items;
        }
        
        public String getName() {
            return name;
        }
        
        public Integer getValue() {
            return value;
        }
        
        public List<String> getItems() {
            return items;
        }
    }
    
    private static class NestedRequest {
        private final String name;
        private final List<TestRequest> children;
        
        public NestedRequest(String name, List<TestRequest> children) {
            this.name = name;
            this.children = children;
        }
        
        public String getName() {
            return name;
        }
        
        public List<TestRequest> getChildren() {
            return children;
        }
    }
}