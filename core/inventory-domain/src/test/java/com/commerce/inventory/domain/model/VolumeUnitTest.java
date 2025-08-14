package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidVolumeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolumeUnitTest {

    @Test
    @DisplayName("유효한 문자열로 VolumeUnit을 생성할 수 있다")
    void shouldCreateVolumeUnitFromValidString() {
        // given
        String unitString = "CUBIC_CM";

        // when
        VolumeUnit unit = VolumeUnit.fromString(unitString);

        // then
        assertThat(unit).isEqualTo(VolumeUnit.CUBIC_CM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"liter", "Liter", "LITER"})
    @DisplayName("대소문자를 구분하지 않고 VolumeUnit을 생성할 수 있다")
    void shouldCreateVolumeUnitIgnoringCase(String unitString) {
        // when
        VolumeUnit unit = VolumeUnit.fromString(unitString);

        // then
        assertThat(unit).isEqualTo(VolumeUnit.LITER);
    }

    @ParameterizedTest
    @ValueSource(strings = {" CUBIC_M", "CUBIC_M ", " CUBIC_M "})
    @DisplayName("앞뒤 공백을 제거하고 VolumeUnit을 생성할 수 있다")
    void shouldCreateVolumeUnitTrimmingSpaces(String unitString) {
        // when
        VolumeUnit unit = VolumeUnit.fromString(unitString);

        // then
        assertThat(unit).isEqualTo(VolumeUnit.CUBIC_M);
    }

    @Test
    @DisplayName("null 문자열로 VolumeUnit을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenStringIsNull() {
        // when & then
        assertThatThrownBy(() -> VolumeUnit.fromString(null))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessage("부피 단위는 null이거나 비어있을 수 없습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열이나 공백 문자열로 VolumeUnit을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenStringIsEmptyOrBlank(String unitString) {
        // when & then
        assertThatThrownBy(() -> VolumeUnit.fromString(unitString))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessage("부피 단위는 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("유효하지 않은 문자열로 VolumeUnit을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenStringIsInvalid() {
        // given
        String invalidUnit = "GALLON";

        // when & then
        assertThatThrownBy(() -> VolumeUnit.fromString(invalidUnit))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessage("유효하지 않은 부피 단위입니다: " + invalidUnit)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}