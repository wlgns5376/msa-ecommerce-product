package com.commerce.product.application.factory;

import com.commerce.product.application.usecase.AddProductOptionRequest;
import com.commerce.product.domain.exception.InvalidProductOptionException;
import com.commerce.product.domain.model.Currency;
import com.commerce.product.domain.model.ProductOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

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
        Map<String, Integer> skuMappings = Map.of("SKU001", 2, "SKU002", 1);

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
        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

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
    @DisplayName("소문자 통화 코드로도 옵션 생성 가능")
    void shouldCreateOptionWithLowerCaseCurrency() {
        // Given
        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
                .productId("test-product-id")
                .optionName("Black - L")
                .price(BigDecimal.valueOf(29900))
                .currency("krw")  // 소문자
                .skuMappings(skuMappings)
                .build();

        // When
        ProductOption option = factory.create(request);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getPrice().currency()).isEqualTo(Currency.KRW);
    }

    @Test
    @DisplayName("혼합된 대소문자 통화 코드로도 옵션 생성 가능")
    void shouldCreateOptionWithMixedCaseCurrency() {
        // Given
        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
                .productId("test-product-id")
                .optionName("Black - L")
                .price(BigDecimal.valueOf(29.99))
                .currency("UsD")  // 혼합된 대소문자
                .skuMappings(skuMappings)
                .build();

        // When
        ProductOption option = factory.create(request);

        // Then
        assertThat(option).isNotNull();
        assertThat(option.getPrice().currency()).isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("유효하지 않은 통화로 옵션 생성 시 예외 발생")
    void shouldThrowExceptionWhenInvalidCurrency() {
        // Given
        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

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
                .hasMessage("유효하지 않은 통화입니다: INVALID_CURRENCY");
    }
}