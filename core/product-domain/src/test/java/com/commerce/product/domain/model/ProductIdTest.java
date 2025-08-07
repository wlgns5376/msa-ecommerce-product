package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdTest {

    @Test
    @DisplayName("유효한 UUID로 ProductId를 생성할 수 있다")
    void shouldCreateProductIdWithValidUuid() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        ProductId productId = new ProductId(validUuid);

        // Then
        assertThat(productId).isNotNull();
        assertThat(productId.getValue()).isEqualTo(validUuid);
    }

    @Test
    @DisplayName("ProductId를 생성할 수 있다")
    void shouldGenerateNewProductId() {
        // When
        ProductId productId = ProductId.generate();

        // Then
        assertThat(productId).isNotNull();
        assertThat(productId.getValue()).isNotNull();
        assertThat(UUID.fromString(productId.getValue())).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("빈 문자열로 ProductId 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithEmptyString(String emptyString) {
        // When & Then
        assertThatThrownBy(() -> new ProductId(emptyString))
                .isInstanceOf(InvalidProductIdException.class)
                .hasMessageContaining("Product ID cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-uuid", "123", "12345678-1234-1234-1234"})
    @DisplayName("유효하지 않은 UUID 형식으로 ProductId 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithInvalidUuidFormat(String invalidUuid) {
        // When & Then
        assertThatThrownBy(() -> new ProductId(invalidUuid))
                .isInstanceOf(InvalidProductIdException.class)
                .hasMessageContaining("Invalid product ID");
    }

    @Test
    @DisplayName("null 값으로 ProductId 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingWithNull() {
        // When & Then
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(InvalidProductIdException.class)
                .hasMessageContaining("Product ID cannot be null or empty");
    }

    @Test
    @DisplayName("동일한 값을 가진 ProductId는 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualProductIds() {
        // Given
        String uuid = UUID.randomUUID().toString();
        ProductId productId1 = new ProductId(uuid);
        ProductId productId2 = new ProductId(uuid);

        // When & Then
        assertThat(productId1).isEqualTo(productId2);
        assertThat(productId1.hashCode()).isEqualTo(productId2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 ProductId는 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentProductIds() {
        // Given
        ProductId productId1 = ProductId.generate();
        ProductId productId2 = ProductId.generate();

        // When & Then
        assertThat(productId1).isNotEqualTo(productId2);
    }

    @Test
    @DisplayName("toString 메서드는 UUID 값을 반환한다")
    void shouldReturnUuidValueWhenCallingToString() {
        // Given
        String uuid = UUID.randomUUID().toString();
        ProductId productId = new ProductId(uuid);

        // When
        String result = productId.toString();

        // Then
        assertThat(result).isEqualTo(uuid);
    }
}