package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;

import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuIdTest {

    @Test
    @DisplayName("유효한 ID로 SkuId를 생성할 수 있다")
    void shouldCreateSkuIdWithValidValue() {
        // given
        String value = "SKU123456789";

        // when
        SkuId skuId = new SkuId(value);

        // then
        assertThat(skuId.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("null이거나 빈 값으로 SkuId를 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenCreateWithInvalidValue(String invalidValue) {
        // when & then
        assertThatThrownBy(() -> new SkuId(invalidValue))
                .isInstanceOf(InvalidSkuIdException.class)
                .hasMessage("SKU ID는 필수입니다");
    }

    @Test
    @DisplayName("SkuId를 생성할 수 있다")
    void shouldGenerateNewSkuId() {
        // when
        SkuId skuId1 = SkuId.generate();
        SkuId skuId2 = SkuId.generate();

        // then
        assertThat(skuId1).isNotNull();
        assertThat(skuId2).isNotNull();
        assertThat(skuId1).isNotEqualTo(skuId2);
    }

    @Test
    @DisplayName("같은 값을 가진 SkuId는 동등하다")
    void shouldBeEqualWhenSameValue() {
        // given
        String value = "SKU123456789";
        SkuId skuId1 = new SkuId(value);
        SkuId skuId2 = new SkuId(value);

        // when & then
        assertThat(skuId1).isEqualTo(skuId2);
        assertThat(skuId1.hashCode()).isEqualTo(skuId2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 SkuId는 동등하지 않다")
    void shouldNotBeEqualWhenDifferentValue() {
        // given
        SkuId skuId1 = new SkuId("SKU123");
        SkuId skuId2 = new SkuId("SKU456");

        // when & then
        assertThat(skuId1).isNotEqualTo(skuId2);
    }
}