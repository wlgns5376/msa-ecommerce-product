package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductNameTest {

    @Test
    @DisplayName("유효한 상품명으로 ProductName을 생성할 수 있다")
    void shouldCreateProductNameWithValidName() {
        // Given
        String validName = "맥북 프로 16인치";

        // When
        ProductName productName = new ProductName(validName);

        // Then
        assertThat(productName).isNotNull();
        assertThat(productName.getValue()).isEqualTo(validName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열로 ProductName 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithEmptyName(String emptyName) {
        // When & Then
        assertThatThrownBy(() -> new ProductName(emptyName))
                .isInstanceOf(InvalidProductNameException.class)
                .hasMessageContaining("Product name cannot be null or empty");
    }

    @Test
    @DisplayName("null 값으로 ProductName 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithNull() {
        // When & Then
        assertThatThrownBy(() -> new ProductName(null))
                .isInstanceOf(InvalidProductNameException.class)
                .hasMessageContaining("Product name cannot be null or empty");
    }

    @Test
    @DisplayName("100자를 초과하는 상품명으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenNameExceedsMaxLength() {
        // Given
        String longName = "a".repeat(101);

        // When & Then
        assertThatThrownBy(() -> new ProductName(longName))
                .isInstanceOf(InvalidProductNameException.class)
                .hasMessageContaining("Product name cannot exceed 100 characters");
    }

    @Test
    @DisplayName("100자 이내의 상품명은 정상적으로 생성된다")
    void shouldCreateProductNameWithMaxLength() {
        // Given
        String maxLengthName = "a".repeat(100);

        // When
        ProductName productName = new ProductName(maxLengthName);

        // Then
        assertThat(productName.getValue()).isEqualTo(maxLengthName);
    }

    @Test
    @DisplayName("동일한 값을 가진 ProductName은 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualProductNames() {
        // Given
        String name = "아이폰 15 Pro";
        ProductName productName1 = new ProductName(name);
        ProductName productName2 = new ProductName(name);

        // When & Then
        assertThat(productName1).isEqualTo(productName2);
        assertThat(productName1.hashCode()).isEqualTo(productName2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 ProductName은 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentProductNames() {
        // Given
        ProductName productName1 = new ProductName("상품1");
        ProductName productName2 = new ProductName("상품2");

        // When & Then
        assertThat(productName1).isNotEqualTo(productName2);
    }

    @Test
    @DisplayName("toString 메서드는 상품명을 반환한다")
    void shouldReturnNameWhenCallingToString() {
        // Given
        String name = "갤럭시 S24 Ultra";
        ProductName productName = new ProductName(name);

        // When
        String result = productName.toString();

        // Then
        assertThat(result).isEqualTo(name);
    }

    @Test
    @DisplayName("특수문자를 포함한 상품명도 생성 가능하다")
    void shouldCreateProductNameWithSpecialCharacters() {
        // Given
        String nameWithSpecialChars = "LG 올레드 TV (55인치) - 2024년형 [특가]";

        // When
        ProductName productName = new ProductName(nameWithSpecialChars);

        // Then
        assertThat(productName.getValue()).isEqualTo(nameWithSpecialChars);
    }
}