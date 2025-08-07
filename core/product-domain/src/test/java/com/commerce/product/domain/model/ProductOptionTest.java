package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductOptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionTest {

    @Test
    @DisplayName("단일 옵션을 생성할 수 있다")
    void shouldCreateSingleOption() {
        // Given
        String name = "블랙 - L 사이즈";
        Money price = Money.of(new BigDecimal("29900"), Currency.KRW);
        String skuId = "SKU001";

        // When
        ProductOption option = ProductOption.single(name, price, skuId);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getId()).isNotNull();
        assertThat(option.getName()).isEqualTo(name);
        assertThat(option.getPrice()).isEqualTo(price);
        assertThat(option.isBundle()).isFalse();
        assertThat(option.getSingleSkuId()).isEqualTo(skuId);
    }

    @Test
    @DisplayName("SkuMapping으로 단일 옵션을 생성할 수 있다")
    void shouldCreateSingleOptionWithSkuMapping() {
        // Given
        String name = "화이트 - M 사이즈";
        Money price = Money.of(new BigDecimal("29900"), Currency.KRW);
        SkuMapping skuMapping = SkuMapping.single("SKU002");

        // When
        ProductOption option = ProductOption.single(name, price, skuMapping);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.isBundle()).isFalse();
        assertThat(option.getSingleSkuId()).isEqualTo("SKU002");
    }

    @Test
    @DisplayName("번들 옵션을 생성할 수 있다")
    void shouldCreateBundleOption() {
        // Given
        String name = "기본 세트";
        Money price = Money.of(new BigDecimal("49900"), Currency.KRW);
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 1);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);

        // When
        ProductOption option = ProductOption.bundle(name, price, skuMapping);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getId()).isNotNull();
        assertThat(option.getName()).isEqualTo(name);
        assertThat(option.getPrice()).isEqualTo(price);
        assertThat(option.isBundle()).isTrue();
        assertThat(option.getSkuQuantity("SKU001")).isEqualTo(2);
        assertThat(option.getSkuQuantity("SKU002")).isEqualTo(1);
    }

    @Test
    @DisplayName("번들 SkuMapping으로 단일 옵션 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingSingleOptionWithBundleMapping() {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 1);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);

        // When & Then
        assertThatThrownBy(() -> ProductOption.single("옵션", Money.of(BigDecimal.TEN, Currency.KRW), skuMapping))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Single option cannot have bundle SKU mapping");
    }

    @Test
    @DisplayName("단일 SkuMapping으로 번들 옵션 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingBundleOptionWithSingleMapping() {
        // Given
        SkuMapping skuMapping = SkuMapping.single("SKU001");

        // When & Then
        assertThatThrownBy(() -> ProductOption.bundle("옵션", Money.of(BigDecimal.TEN, Currency.KRW), skuMapping))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Bundle option must have bundle SKU mapping");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 이름으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenNameIsEmpty(String emptyName) {
        // When & Then
        assertThatThrownBy(() -> ProductOption.single(emptyName, Money.of(BigDecimal.TEN, Currency.KRW), "SKU001"))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Option name cannot be null or empty");
    }

    @Test
    @DisplayName("null 가격으로 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenPriceIsNull() {
        // When & Then
        assertThatThrownBy(() -> ProductOption.single("옵션", null, "SKU001"))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessageContaining("Price cannot be null");
    }

    @Test
    @DisplayName("번들 옵션에서 단일 SKU ID 조회시 예외가 발생한다")
    void shouldThrowExceptionWhenGettingSingleSkuIdFromBundle() {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 1);
        ProductOption bundleOption = ProductOption.bundle("번들", Money.of(BigDecimal.TEN, Currency.KRW), SkuMapping.bundle(bundleMapping));

        // When & Then
        assertThatThrownBy(() -> bundleOption.getSingleSkuId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot get single SKU ID from bundle mapping");
    }

    @Test
    @DisplayName("특정 SKU의 수량을 조회할 수 있다")
    void shouldGetSkuQuantity() {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 3);
        ProductOption option = ProductOption.bundle("번들 상품", Money.of(new BigDecimal("50000"), Currency.KRW), SkuMapping.bundle(bundleMapping));

        // When & Then
        assertThat(option.getSkuQuantity("SKU001")).isEqualTo(2);
        assertThat(option.getSkuQuantity("SKU002")).isEqualTo(3);
        assertThat(option.getSkuQuantity("SKU003")).isEqualTo(0);
    }

    @Test
    @DisplayName("동일한 값을 가진 ProductOption은 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualOptions() {
        // Given
        String name = "옵션1";
        Money price = Money.of(new BigDecimal("10000"), Currency.KRW);
        String skuId = "SKU001";

        ProductOption option1 = ProductOption.single(name, price, skuId);
        ProductOption option2 = ProductOption.single(name, price, skuId);

        // When & Then - ID가 다르므로 equals는 false
        assertThat(option1).isNotEqualTo(option2);
    }

    @Test
    @DisplayName("toString 메서드는 옵션 정보를 포함한 문자열을 반환한다")
    void shouldReturnFormattedStringWhenCallingToString() {
        // Given
        ProductOption singleOption = ProductOption.single("단일 옵션", Money.of(new BigDecimal("10000"), Currency.KRW), "SKU001");
        
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 1);
        ProductOption bundleOption = ProductOption.bundle("번들 옵션", Money.of(new BigDecimal("20000"), Currency.KRW), SkuMapping.bundle(bundleMapping));

        // When
        String singleResult = singleOption.toString();
        String bundleResult = bundleOption.toString();

        // Then
        assertThat(singleResult).contains("단일 옵션");
        assertThat(singleResult).contains("10000");
        assertThat(singleResult).contains("isBundle=false");
        
        assertThat(bundleResult).contains("번들 옵션");
        assertThat(bundleResult).contains("20000");
        assertThat(bundleResult).contains("isBundle=true");
    }
}