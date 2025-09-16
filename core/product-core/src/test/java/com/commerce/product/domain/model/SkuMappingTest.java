package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidSkuMappingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuMappingTest {

    @Test
    @DisplayName("단일 SKU 매핑을 생성할 수 있다")
    void shouldCreateSingleSkuMapping() {
        // When
        SkuMapping skuMapping = SkuMapping.single("SKU001");

        // Then
        assertThat(skuMapping).isNotNull();
        assertThat(skuMapping.isBundle()).isFalse();
        assertThat(skuMapping.getSingleSkuId()).isEqualTo("SKU001");
        assertThat(skuMapping.getQuantityForSku("SKU001")).isEqualTo(1);
    }

    @Test
    @DisplayName("null SKU ID로 단일 매핑 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenSingleSkuIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> SkuMapping.single(null))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("SKU ID cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 SKU ID로 단일 매핑 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenSingleSkuIdIsEmpty(String emptySkuId) {
        // When & Then
        assertThatThrownBy(() -> SkuMapping.single(emptySkuId))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("SKU ID cannot be null or empty");
    }

    @Test
    @DisplayName("번들 SKU 매핑을 생성할 수 있다")
    void shouldCreateBundleSkuMapping() {
        // Given
        Map<String, Integer> mappings = new HashMap<>();
        mappings.put("SKU001", 2);
        mappings.put("SKU002", 1);

        // When
        SkuMapping skuMapping = SkuMapping.bundle(mappings);

        // Then
        assertThat(skuMapping).isNotNull();
        assertThat(skuMapping.isBundle()).isTrue();
        assertThat(skuMapping.getQuantityForSku("SKU001")).isEqualTo(2);
        assertThat(skuMapping.getQuantityForSku("SKU002")).isEqualTo(1);
    }

    @Test
    @DisplayName("null 매핑으로 번들 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenBundleMappingsIsNull() {
        // When & Then
        assertThatThrownBy(() -> SkuMapping.bundle(null))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("Bundle mappings cannot be null or empty");
    }

    @Test
    @DisplayName("빈 매핑으로 번들 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenBundleMappingsIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> SkuMapping.bundle(new HashMap<>()))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("Bundle mappings cannot be null or empty");
    }

    @Test
    @DisplayName("SKU가 하나만 있는 매핑으로 번들 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenBundleHasOnlyOneSku() {
        // Given
        Map<String, Integer> mappings = new HashMap<>();
        mappings.put("SKU001", 1);

        // When & Then
        assertThatThrownBy(() -> SkuMapping.bundle(mappings))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("Bundle must contain at least 2 SKUs");
    }

    @Test
    @DisplayName("번들에서 단일 SKU ID 조회시 예외가 발생한다")
    void shouldThrowExceptionWhenGettingSingleSkuIdFromBundle() {
        // Given
        Map<String, Integer> mappings = new HashMap<>();
        mappings.put("SKU001", 2);
        mappings.put("SKU002", 1);
        SkuMapping bundleMapping = SkuMapping.bundle(mappings);

        // When & Then
        assertThatThrownBy(() -> bundleMapping.getSingleSkuId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot get single SKU ID from bundle mapping");
    }

    @Test
    @DisplayName("존재하지 않는 SKU의 수량 조회시 0을 반환한다")
    void shouldReturnZeroForNonExistentSku() {
        // Given
        SkuMapping skuMapping = SkuMapping.single("SKU001");

        // When
        int quantity = skuMapping.getQuantityForSku("SKU999");

        // Then
        assertThat(quantity).isEqualTo(0);
    }

    @Test
    @DisplayName("번들 매핑에 0 이하의 수량이 있으면 예외가 발생한다")
    void shouldThrowExceptionWhenBundleHasInvalidQuantity() {
        // Given
        Map<String, Integer> mappings = new HashMap<>();
        mappings.put("SKU001", 2);
        mappings.put("SKU002", 0);

        // When & Then
        assertThatThrownBy(() -> SkuMapping.bundle(mappings))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("Quantity must be positive for SKU: SKU002");
    }

    @Test
    @DisplayName("번들 매핑에 null SKU ID가 있으면 예외가 발생한다")
    void shouldThrowExceptionWhenBundleHasNullSkuId() {
        // Given
        Map<String, Integer> mappings = new HashMap<>();
        mappings.put("SKU001", 2);
        mappings.put(null, 1);

        // When & Then
        assertThatThrownBy(() -> SkuMapping.bundle(mappings))
                .isInstanceOf(InvalidSkuMappingException.class)
                .hasMessageContaining("SKU ID cannot be null or empty");
    }

    @Test
    @DisplayName("동일한 매핑을 가진 SkuMapping은 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualSkuMappings() {
        // Given
        Map<String, Integer> mappings1 = new HashMap<>();
        mappings1.put("SKU001", 2);
        mappings1.put("SKU002", 1);
        
        Map<String, Integer> mappings2 = new HashMap<>();
        mappings2.put("SKU001", 2);
        mappings2.put("SKU002", 1);

        SkuMapping mapping1 = SkuMapping.bundle(mappings1);
        SkuMapping mapping2 = SkuMapping.bundle(mappings2);

        // When & Then
        assertThat(mapping1).isEqualTo(mapping2);
        assertThat(mapping1.hashCode()).isEqualTo(mapping2.hashCode());
    }

    @Test
    @DisplayName("매핑이 불변임을 확인한다")
    void shouldEnsureMappingsAreImmutable() {
        // Given
        Map<String, Integer> originalMappings = new HashMap<>();
        originalMappings.put("SKU001", 2);
        originalMappings.put("SKU002", 1);
        
        SkuMapping skuMapping = SkuMapping.bundle(originalMappings);
        
        // When - 원본 맵 수정
        originalMappings.put("SKU003", 3);
        
        // Then - SkuMapping은 영향받지 않음
        assertThat(skuMapping.mappings()).hasSize(2);
        assertThat(skuMapping.getQuantityForSku("SKU003")).isEqualTo(0);
        
        // When & Then - 반환된 맵 수정 시도
        assertThatThrownBy(() -> skuMapping.mappings().put("SKU004", 4))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("toString 메서드는 매핑 정보를 포함한 문자열을 반환한다")
    void shouldReturnFormattedStringWhenCallingToString() {
        // Given
        SkuMapping singleMapping = SkuMapping.single("SKU001");
        
        Map<String, Integer> bundleMappings = new HashMap<>();
        bundleMappings.put("SKU001", 2);
        bundleMappings.put("SKU002", 1);
        SkuMapping bundleMapping = SkuMapping.bundle(bundleMappings);

        // When
        String singleResult = singleMapping.toString();
        String bundleResult = bundleMapping.toString();

        // Then
        assertThat(singleResult).contains("SKU001");
        assertThat(singleResult).contains("isBundle=false");
        
        assertThat(bundleResult).contains("SKU001");
        assertThat(bundleResult).contains("SKU002");
        assertThat(bundleResult).contains("isBundle=true");
    }
}