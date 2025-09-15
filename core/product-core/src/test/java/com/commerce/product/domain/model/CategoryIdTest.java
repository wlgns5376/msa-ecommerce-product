package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryIdTest {

    @Test
    @DisplayName("유효한 값으로 CategoryId를 생성할 수 있다")
    void shouldCreateCategoryIdWithValidValue() {
        // Given
        String validId = "CAT001";

        // When
        CategoryId categoryId = new CategoryId(validId);

        // Then
        assertThat(categoryId).isNotNull();
        assertThat(categoryId.value()).isEqualTo(validId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열로 CategoryId 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithEmptyString(String emptyId) {
        // When & Then
        assertThatThrownBy(() -> new CategoryId(emptyId))
                .isInstanceOf(InvalidCategoryIdException.class)
                .hasMessageContaining("Category ID cannot be null or empty");
    }

    @Test
    @DisplayName("null 값으로 CategoryId 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithNull() {
        // When & Then
        assertThatThrownBy(() -> new CategoryId(null))
                .isInstanceOf(InvalidCategoryIdException.class)
                .hasMessageContaining("Category ID cannot be null or empty");
    }

    @Test
    @DisplayName("동일한 값을 가진 CategoryId는 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualCategoryIds() {
        // Given
        String id = "CAT001";
        CategoryId categoryId1 = new CategoryId(id);
        CategoryId categoryId2 = new CategoryId(id);

        // When & Then
        assertThat(categoryId1).isEqualTo(categoryId2);
        assertThat(categoryId1.hashCode()).isEqualTo(categoryId2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 CategoryId는 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentCategoryIds() {
        // Given
        CategoryId categoryId1 = new CategoryId("CAT001");
        CategoryId categoryId2 = new CategoryId("CAT002");

        // When & Then
        assertThat(categoryId1).isNotEqualTo(categoryId2);
    }

    @Test
    @DisplayName("toString 메서드는 record 형식으로 값을 반환한다")
    void shouldReturnRecordFormatWhenCallingToString() {
        // Given
        String id = "CAT001";
        CategoryId categoryId = new CategoryId(id);

        // When
        String result = categoryId.toString();

        // Then
        assertThat(result).isEqualTo("CategoryId[value=" + id + "]");
    }
}