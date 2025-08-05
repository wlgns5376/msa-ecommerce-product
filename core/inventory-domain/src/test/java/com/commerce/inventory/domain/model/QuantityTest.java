package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    @DisplayName("유효한 수량으로 Quantity를 생성할 수 있다")
    void shouldCreateQuantityWithValidValue() {
        // given
        int value = 100;

        // when
        Quantity quantity = new Quantity(value);

        // then
        assertThat(quantity.getValue()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("음수 수량으로 Quantity를 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenCreateWithNegativeValue(int negativeValue) {
        // when & then
        assertThatThrownBy(() -> new Quantity(negativeValue))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessage("수량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("0 수량으로 Quantity를 생성할 수 있다")
    void shouldCreateQuantityWithZero() {
        // when
        Quantity quantity = new Quantity(0);

        // then
        assertThat(quantity.getValue()).isZero();
    }

    @Test
    @DisplayName("두 Quantity를 더할 수 있다")
    void shouldAddTwoQuantities() {
        // given
        Quantity quantity1 = new Quantity(50);
        Quantity quantity2 = new Quantity(30);

        // when
        Quantity result = quantity1.add(quantity2);

        // then
        assertThat(result.getValue()).isEqualTo(80);
    }

    @Test
    @DisplayName("두 Quantity를 뺄 수 있다")
    void shouldSubtractTwoQuantities() {
        // given
        Quantity quantity1 = new Quantity(100);
        Quantity quantity2 = new Quantity(30);

        // when
        Quantity result = quantity1.subtract(quantity2);

        // then
        assertThat(result.getValue()).isEqualTo(70);
    }

    @Test
    @DisplayName("더 큰 수량을 빼면 예외가 발생한다")
    void shouldThrowExceptionWhenSubtractResultIsNegative() {
        // given
        Quantity quantity1 = new Quantity(30);
        Quantity quantity2 = new Quantity(50);

        // when & then
        assertThatThrownBy(() -> quantity1.subtract(quantity2))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessage("수량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("Quantity 비교 - 크거나 같은지 확인할 수 있다")
    void shouldCompareQuantities() {
        // given
        Quantity quantity1 = new Quantity(100);
        Quantity quantity2 = new Quantity(50);
        Quantity quantity3 = new Quantity(100);

        // when & then
        assertThat(quantity1.isGreaterThanOrEqual(quantity2)).isTrue();
        assertThat(quantity1.isGreaterThanOrEqual(quantity3)).isTrue();
        assertThat(quantity2.isGreaterThanOrEqual(quantity1)).isFalse();
    }

    @Test
    @DisplayName("같은 값을 가진 Quantity는 동등하다")
    void shouldBeEqualWhenSameValue() {
        // given
        Quantity quantity1 = new Quantity(100);
        Quantity quantity2 = new Quantity(100);

        // when & then
        assertThat(quantity1).isEqualTo(quantity2);
        assertThat(quantity1.hashCode()).isEqualTo(quantity2.hashCode());
    }
}