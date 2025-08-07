package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidSkuMappingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuMappingTest {

    @Test
    @DisplayName("유효한 SKU ID와 수량으로 SkuMapping을 생성할 수 있다")
    void shouldCreateSkuMappingWithValidValues() {
        // Given
        String skuId = "SKU001";
        int quantity = 2;

        // When
        SkuMapping skuMapping = new SkuMapping(skuId, quantity);

        // Then
        assertThat(skuMapping).isNotNull();
        assertThat(skuMapping.getSkuId()).isEqualTo(skuId);
        assertThat(skuMapping.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("null SKU ID로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> new SkuMapping(null, 1))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("SKU ID cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 SKU ID로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsEmpty(String emptySkuId) {
        // When & Then
        assertThatThrownBy(() -> new SkuMapping(emptySkuId, 1))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("SKU ID cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("0 이하의 수량으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenQuantityIsNotPositive(int invalidQuantity) {
        // When & Then
        assertThatThrownBy(() -> new SkuMapping("SKU001", invalidQuantity))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    @DisplayName("수량 1로 생성할 수 있다")
    void shouldCreateSkuMappingWithQuantityOne() {
        // When
        SkuMapping skuMapping = new SkuMapping("SKU001", 1);

        // Then
        assertThat(skuMapping.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 SKU ID와 수량을 가진 SkuMapping은 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualSkuMappings() {
        // Given
        SkuMapping mapping1 = new SkuMapping("SKU001", 2);
        SkuMapping mapping2 = new SkuMapping("SKU001", 2);

        // When & Then
        assertThat(mapping1).isEqualTo(mapping2);
        assertThat(mapping1.hashCode()).isEqualTo(mapping2.hashCode());
    }

    @Test
    @DisplayName("다른 SKU ID를 가진 SkuMapping은 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentSkuIds() {
        // Given
        SkuMapping mapping1 = new SkuMapping("SKU001", 2);
        SkuMapping mapping2 = new SkuMapping("SKU002", 2);

        // When & Then
        assertThat(mapping1).isNotEqualTo(mapping2);
    }

    @Test
    @DisplayName("다른 수량을 가진 SkuMapping은 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentQuantities() {
        // Given
        SkuMapping mapping1 = new SkuMapping("SKU001", 1);
        SkuMapping mapping2 = new SkuMapping("SKU001", 2);

        // When & Then
        assertThat(mapping1).isNotEqualTo(mapping2);
    }

    @Test
    @DisplayName("toString 메서드는 SKU ID와 수량을 포함한 문자열을 반환한다")
    void shouldReturnFormattedStringWhenCallingToString() {
        // Given
        SkuMapping skuMapping = new SkuMapping("SKU001", 3);

        // When
        String result = skuMapping.toString();

        // Then
        assertThat(result).isEqualTo("SkuMapping{skuId='SKU001', quantity=3}");
    }
}