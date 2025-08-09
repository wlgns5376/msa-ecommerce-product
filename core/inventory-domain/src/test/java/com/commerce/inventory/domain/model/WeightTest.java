package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;

import com.commerce.inventory.domain.exception.InvalidWeightException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightTest {

    @Test
    @DisplayName("유효한 값으로 Weight를 생성할 수 있다")
    void shouldCreateWeightWithValidValue() {
        // given
        double value = 150.5;
        WeightUnit unit = WeightUnit.GRAM;

        // when
        Weight weight = new Weight(value, unit);

        // then
        assertThat(weight.value()).isEqualTo(value);
        assertThat(weight.unit()).isEqualTo(unit);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -100.5})
    @DisplayName("음수 값으로 Weight를 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenCreateWithNegativeValue(double negativeValue) {
        // when & then
        assertThatThrownBy(() -> new Weight(negativeValue, WeightUnit.GRAM))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("무게는 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("단위가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenUnitIsNull() {
        // when & then
        assertThatThrownBy(() -> new Weight(100.0, null))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("무게 단위는 필수입니다");
    }

    @ParameterizedTest
    @EnumSource(WeightUnit.class)
    @DisplayName("모든 무게 단위로 Weight를 생성할 수 있다")
    void shouldCreateWeightWithAllUnits(WeightUnit unit) {
        // when
        Weight weight = new Weight(100.0, unit);

        // then
        assertThat(weight.unit()).isEqualTo(unit);
    }

    @Test
    @DisplayName("같은 값과 단위를 가진 Weight는 동등하다")
    void shouldBeEqualWhenSameValueAndUnit() {
        // given
        Weight weight1 = new Weight(100.0, WeightUnit.GRAM);
        Weight weight2 = new Weight(100.0, WeightUnit.GRAM);

        // when & then
        assertThat(weight1).isEqualTo(weight2);
        assertThat(weight1.hashCode()).isEqualTo(weight2.hashCode());
    }
}