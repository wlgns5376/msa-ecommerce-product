package com.commerce.product.application.factory;

import com.commerce.product.application.usecase.AddProductOptionRequest;
import com.commerce.product.domain.exception.InvalidProductOptionException;
import com.commerce.product.domain.model.Currency;
import com.commerce.product.domain.model.ProductOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionFactoryTest {

    private ProductOptionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ProductOptionFactory();
    }

    @Test
    @DisplayName("단일 상품 옵션 생성")
    void shouldCreateSingleProductOption() {
        // Given
        Map<String, Integer> skuMappings = new HashMap<>();
        skuMappings.put("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
                .productId("test-product-id")
                .optionName("블랙 - L")
                .price(BigDecimal.valueOf(29900))
                .currency("KRW")
                .skuMappings(skuMappings)
                .build();

        // When
        ProductOption option = factory.create(request);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getName()).isEqualTo("블랙 - L");
        assertThat(option.getPrice().amount()).isEqualTo(BigDecimal.valueOf(29900));
        assertThat(option.getPrice().currency()).isEqualTo(Currency.KRW);
        assertThat(option.isBundle()).isFalse();
    }

    @Test
    @DisplayName("묶음 상품 옵션 생성")
    void shouldCreateBundleProductOption() {
        // Given
        Map<String, Integer> skuMappings = new HashMap<>();
        skuMappings.put("SKU001", 2);
        skuMappings.put("SKU002", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
                .productId("test-product-id")
                .optionName("A+B 세트")
                .price(BigDecimal.valueOf(39900))
                .currency("KRW")
                .skuMappings(skuMappings)
                .build();

        // When
        ProductOption option = factory.create(request);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getName()).isEqualTo("A+B 세트");
        assertThat(option.getPrice().amount()).isEqualTo(BigDecimal.valueOf(39900));
        assertThat(option.getPrice().currency()).isEqualTo(Currency.KRW);
        assertThat(option.isBundle()).isTrue();
    }

    @Test
    @DisplayName("USD 통화로 옵션 생성")
    void shouldCreateOptionWithUSDCurrency() {
        // Given
        Map<String, Integer> skuMappings = new HashMap<>();
        skuMappings.put("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
                .productId("test-product-id")
                .optionName("Black - L")
                .price(BigDecimal.valueOf(29.99))
                .currency("USD")
                .skuMappings(skuMappings)
                .build();

        // When
        ProductOption option = factory.create(request);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getPrice().currency()).isEqualTo(Currency.USD);
        assertThat(option.getPrice().amount()).isEqualTo(BigDecimal.valueOf(29.99));
    }

    @Test
    @DisplayName("유효하지 않은 통화로 옵션 생성 시 예외 발생")
    void shouldThrowExceptionWhenInvalidCurrency() {
        // Given
        Map<String, Integer> skuMappings = new HashMap<>();
        skuMappings.put("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
                .productId("test-product-id")
                .optionName("Black - L")
                .price(BigDecimal.valueOf(29900))
                .currency("INVALID_CURRENCY")
                .skuMappings(skuMappings)
                .build();

        // When & Then
        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(InvalidProductOptionException.class)
                .hasMessage("Invalid currency: INVALID_CURRENCY");
    }
}