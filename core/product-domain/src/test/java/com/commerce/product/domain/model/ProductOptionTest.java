package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductOptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionTest {

    @Test
    @DisplayName("유효한 값으로 ProductOption을 생성할 수 있다")
    void shouldCreateProductOptionWithValidValues() {
        // Given
        String id = UUID.randomUUID().toString();
        String name = "블랙 - L 사이즈";
        Money price = new Money(new BigDecimal("29900"));
        List<SkuMapping> skuMappings = Arrays.asList(
                new SkuMapping("SKU001", 1)
        );

        // When
        ProductOption option = new ProductOption(id, name, price, skuMappings);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getId()).isEqualTo(id);
        assertThat(option.getName()).isEqualTo(name);
        assertThat(option.getPrice()).isEqualTo(price);
        assertThat(option.getSkuMappings()).hasSize(1);
    }

    @Test
    @DisplayName("새로운 ProductOption을 생성할 수 있다")
    void shouldGenerateNewProductOption() {
        // Given
        String name = "화이트 - M 사이즈";
        Money price = new Money(new BigDecimal("29900"));
        List<SkuMapping> skuMappings = Arrays.asList(
                new SkuMapping("SKU002", 1)
        );

        // When
        ProductOption option = ProductOption.create(name, price, skuMappings);

        // Then
        assertThat(option.getId()).isNotNull();
        assertThat(UUID.fromString(option.getId())).isNotNull();
    }

    @Test
    @DisplayName("null ID로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenIdIsNull() {
        // Given
        Money price = new Money(new BigDecimal("10000"));
        List<SkuMapping> skuMappings = Arrays.asList(new SkuMapping("SKU001", 1));

        // When & Then
        assertThatThrownBy(() -> new ProductOption(null, "옵션", price, skuMappings))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Option ID cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 이름으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenNameIsEmpty(String emptyName) {
        // Given
        String id = UUID.randomUUID().toString();
        Money price = new Money(new BigDecimal("10000"));
        List<SkuMapping> skuMappings = Arrays.asList(new SkuMapping("SKU001", 1));

        // When & Then
        assertThatThrownBy(() -> new ProductOption(id, emptyName, price, skuMappings))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Option name cannot be null or empty");
    }

    @Test
    @DisplayName("null 가격으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenPriceIsNull() {
        // Given
        String id = UUID.randomUUID().toString();
        List<SkuMapping> skuMappings = Arrays.asList(new SkuMapping("SKU001", 1));

        // When & Then
        assertThatThrownBy(() -> new ProductOption(id, "옵션", null, skuMappings))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Price cannot be null");
    }

    @Test
    @DisplayName("빈 SKU 매핑 목록으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenSkuMappingsIsEmpty() {
        // Given
        String id = UUID.randomUUID().toString();
        Money price = new Money(new BigDecimal("10000"));

        // When & Then
        assertThatThrownBy(() -> new ProductOption(id, "옵션", price, Collections.emptyList()))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Option must have at least one SKU mapping");
    }

    @Test
    @DisplayName("null SKU 매핑 목록으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenSkuMappingsIsNull() {
        // Given
        String id = UUID.randomUUID().toString();
        Money price = new Money(new BigDecimal("10000"));

        // When & Then
        assertThatThrownBy(() -> new ProductOption(id, "옵션", price, null))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Option must have at least one SKU mapping");
    }

    @Test
    @DisplayName("단일 SKU를 가진 옵션인지 확인할 수 있다")
    void shouldIdentifySingleSkuOption() {
        // Given
        ProductOption singleSkuOption = createOptionWithSkuCount(1);
        ProductOption multipleSkuOption = createOptionWithSkuCount(2);

        // When & Then
        assertThat(singleSkuOption.hasMultipleSkus()).isFalse();
        assertThat(multipleSkuOption.hasMultipleSkus()).isTrue();
    }

    @Test
    @DisplayName("특정 SKU의 총 수량을 계산할 수 있다")
    void shouldCalculateTotalQuantityForSku() {
        // Given
        List<SkuMapping> skuMappings = Arrays.asList(
                new SkuMapping("SKU001", 2),
                new SkuMapping("SKU002", 3)
        );
        ProductOption option = ProductOption.create("번들 상품", new Money(new BigDecimal("50000")), skuMappings);

        // When & Then
        assertThat(option.getTotalSkuQuantity("SKU001")).isEqualTo(2);
        assertThat(option.getTotalSkuQuantity("SKU002")).isEqualTo(3);
        assertThat(option.getTotalSkuQuantity("SKU003")).isEqualTo(0);
    }

    @Test
    @DisplayName("동일한 값을 가진 ProductOption은 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualOptions() {
        // Given
        String id = UUID.randomUUID().toString();
        String name = "옵션1";
        Money price = new Money(new BigDecimal("10000"));
        List<SkuMapping> skuMappings = Arrays.asList(new SkuMapping("SKU001", 1));

        ProductOption option1 = new ProductOption(id, name, price, skuMappings);
        ProductOption option2 = new ProductOption(id, name, price, skuMappings);

        // When & Then
        assertThat(option1).isEqualTo(option2);
        assertThat(option1.hashCode()).isEqualTo(option2.hashCode());
    }

    @Test
    @DisplayName("다른 ID를 가진 ProductOption은 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentIds() {
        // Given
        Money price = new Money(new BigDecimal("10000"));
        List<SkuMapping> skuMappings = Arrays.asList(new SkuMapping("SKU001", 1));

        ProductOption option1 = ProductOption.create("옵션", price, skuMappings);
        ProductOption option2 = ProductOption.create("옵션", price, skuMappings);

        // When & Then
        assertThat(option1).isNotEqualTo(option2);
    }

    @Test
    @DisplayName("SKU 매핑 목록은 불변이다")
    void shouldHaveImmutableSkuMappings() {
        // Given
        List<SkuMapping> originalMappings = Arrays.asList(new SkuMapping("SKU001", 1));
        ProductOption option = ProductOption.create("옵션", new Money(new BigDecimal("10000")), originalMappings);

        // When
        List<SkuMapping> retrievedMappings = option.getSkuMappings();

        // Then
        assertThatThrownBy(() -> retrievedMappings.add(new SkuMapping("SKU002", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ProductOption createOptionWithSkuCount(int skuCount) {
        List<SkuMapping> mappings = new java.util.ArrayList<>();
        for (int i = 1; i <= skuCount; i++) {
            mappings.add(new SkuMapping("SKU00" + i, 1));
        }
        return ProductOption.create("테스트 옵션", new Money(new BigDecimal("10000")), mappings);
    }
}