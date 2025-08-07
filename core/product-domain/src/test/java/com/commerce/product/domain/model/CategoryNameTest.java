package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidCategoryNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryNameTest {

    @Test
    @DisplayName("유효한 카테고리명으로 CategoryName을 생성할 수 있다")
    void shouldCreateCategoryNameWithValidName() {
        // Given
        String validName = "전자제품";

        // When
        CategoryName categoryName = new CategoryName(validName);

        // Then
        assertThat(categoryName).isNotNull();
        assertThat(categoryName.getValue()).isEqualTo(validName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열로 CategoryName 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithEmptyName(String emptyName) {
        // When & Then
        assertThatThrownBy(() -> new CategoryName(emptyName))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("Category name cannot be null or empty");
    }

    @Test
    @DisplayName("null 값으로 CategoryName 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithNull() {
        // When & Then
        assertThatThrownBy(() -> new CategoryName(null))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("Category name cannot be null or empty");
    }

    @Test
    @DisplayName("50자를 초과하는 카테고리명으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenNameExceedsMaxLength() {
        // Given
        String longName = "a".repeat(51);

        // When & Then
        assertThatThrownBy(() -> new CategoryName(longName))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("Category name cannot exceed 50 characters");
    }

    @Test
    @DisplayName("50자 이내의 카테고리명은 정상적으로 생성된다")
    void shouldCreateCategoryNameWithMaxLength() {
        // Given
        String maxLengthName = "a".repeat(50);

        // When
        CategoryName categoryName = new CategoryName(maxLengthName);

        // Then
        assertThat(categoryName.getValue()).isEqualTo(maxLengthName);
    }

    @Test
    @DisplayName("동일한 값을 가진 CategoryName은 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualCategoryNames() {
        // Given
        String name = "컴퓨터/노트북";
        CategoryName categoryName1 = new CategoryName(name);
        CategoryName categoryName2 = new CategoryName(name);

        // When & Then
        assertThat(categoryName1).isEqualTo(categoryName2);
        assertThat(categoryName1.hashCode()).isEqualTo(categoryName2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 CategoryName은 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentCategoryNames() {
        // Given
        CategoryName categoryName1 = new CategoryName("전자제품");
        CategoryName categoryName2 = new CategoryName("가전제품");

        // When & Then
        assertThat(categoryName1).isNotEqualTo(categoryName2);
    }

    @Test
    @DisplayName("toString 메서드는 카테고리명을 반환한다")
    void shouldReturnNameWhenCallingToString() {
        // Given
        String name = "스마트폰/태블릿";
        CategoryName categoryName = new CategoryName(name);

        // When
        String result = categoryName.toString();

        // Then
        assertThat(result).isEqualTo(name);
    }

    @Test
    @DisplayName("특수문자를 포함한 카테고리명도 생성 가능하다")
    void shouldCreateCategoryNameWithSpecialCharacters() {
        // Given
        String nameWithSpecialChars = "TV/영상가전 & 오디오";

        // When
        CategoryName categoryName = new CategoryName(nameWithSpecialChars);

        // Then
        assertThat(categoryName.getValue()).isEqualTo(nameWithSpecialChars);
    }
}