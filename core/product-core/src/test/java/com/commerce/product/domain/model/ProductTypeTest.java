package com.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTypeTest {

    @Test
    @DisplayName("ProductType enum은 NORMAL과 BUNDLE 값을 가진다")
    void shouldHaveNormalAndBundleTypes() {
        // When & Then
        assertThat(ProductType.values()).hasSize(2);
        assertThat(ProductType.valueOf("NORMAL")).isEqualTo(ProductType.NORMAL);
        assertThat(ProductType.valueOf("BUNDLE")).isEqualTo(ProductType.BUNDLE);
    }

    @Test
    @DisplayName("NORMAL 타입은 일반 상품을 나타낸다")
    void shouldRepresentNormalProduct() {
        // Given
        ProductType type = ProductType.NORMAL;

        // When & Then
        assertThat(type).isEqualTo(ProductType.NORMAL);
        assertThat(type.name()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("BUNDLE 타입은 묶음 상품을 나타낸다")
    void shouldRepresentBundleProduct() {
        // Given
        ProductType type = ProductType.BUNDLE;

        // When & Then
        assertThat(type).isEqualTo(ProductType.BUNDLE);
        assertThat(type.name()).isEqualTo("BUNDLE");
    }

    @Test
    @DisplayName("ProductType은 문자열로 변환 가능하다")
    void shouldConvertToString() {
        // When & Then
        assertThat(ProductType.NORMAL.toString()).isEqualTo("NORMAL");
        assertThat(ProductType.BUNDLE.toString()).isEqualTo("BUNDLE");
    }
}