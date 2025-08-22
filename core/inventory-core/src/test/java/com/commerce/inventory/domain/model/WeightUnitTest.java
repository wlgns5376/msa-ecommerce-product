package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidWeightException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightUnitTest {

    @Test
    @DisplayName("유효한 문자열로 WeightUnit을 생성할 수 있다")
    void shouldCreateWeightUnitFromValidString() {
        // given
        String unitString = "KILOGRAM";

        // when
        WeightUnit unit = WeightUnit.fromString(unitString);

        // then
        assertThat(unit).isEqualTo(WeightUnit.KILOGRAM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gram", "Gram", "GRAM"})
    @DisplayName("대소문자를 구분하지 않고 WeightUnit을 생성할 수 있다")
    void shouldCreateWeightUnitIgnoringCase(String unitString) {
        // when
        WeightUnit unit = WeightUnit.fromString(unitString);

        // then
        assertThat(unit).isEqualTo(WeightUnit.GRAM);
    }

    @ParameterizedTest
    @ValueSource(strings = {" KILOGRAM", "KILOGRAM ", " KILOGRAM "})
    @DisplayName("앞뒤 공백을 제거하고 WeightUnit을 생성할 수 있다")
    void shouldCreateWeightUnitTrimmingSpaces(String unitString) {
        // when
        WeightUnit unit = WeightUnit.fromString(unitString);

        // then
        assertThat(unit).isEqualTo(WeightUnit.KILOGRAM);
    }

    @Test
    @DisplayName("null 문자열로 WeightUnit을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenStringIsNull() {
        // when & then
        assertThatThrownBy(() -> WeightUnit.fromString(null))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("무게 단위는 null이거나 비어있을 수 없습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열이나 공백 문자열로 WeightUnit을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenStringIsEmptyOrBlank(String unitString) {
        // when & then
        assertThatThrownBy(() -> WeightUnit.fromString(unitString))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("무게 단위는 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("유효하지 않은 문자열로 WeightUnit을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenStringIsInvalid() {
        // given
        String invalidUnit = "POUND";

        // when & then
        assertThatThrownBy(() -> WeightUnit.fromString(invalidUnit))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("유효하지 않은 무게 단위입니다: " + invalidUnit)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}