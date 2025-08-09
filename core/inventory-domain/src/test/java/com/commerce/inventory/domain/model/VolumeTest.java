package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;

import com.commerce.inventory.domain.exception.InvalidVolumeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolumeTest {

    @Test
    @DisplayName("유효한 값으로 Volume을 생성할 수 있다")
    void shouldCreateVolumeWithValidValue() {
        // given
        double value = 1000.0;
        VolumeUnit unit = VolumeUnit.CUBIC_CM;

        // when
        Volume volume = new Volume(value, unit);

        // then
        assertThat(volume.value()).isEqualTo(value);
        assertThat(volume.unit()).isEqualTo(unit);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -100.5})
    @DisplayName("음수 값으로 Volume을 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenCreateWithNegativeValue(double negativeValue) {
        // when & then
        assertThatThrownBy(() -> new Volume(negativeValue, VolumeUnit.CUBIC_CM))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessage("부피는 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("단위가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenUnitIsNull() {
        // when & then
        assertThatThrownBy(() -> new Volume(100.0, null))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessage("부피 단위는 필수입니다");
    }

    @ParameterizedTest
    @EnumSource(VolumeUnit.class)
    @DisplayName("모든 부피 단위로 Volume을 생성할 수 있다")
    void shouldCreateVolumeWithAllUnits(VolumeUnit unit) {
        // when
        Volume volume = new Volume(100.0, unit);

        // then
        assertThat(volume.unit()).isEqualTo(unit);
    }

    @Test
    @DisplayName("같은 값과 단위를 가진 Volume은 동등하다")
    void shouldBeEqualWhenSameValueAndUnit() {
        // given
        Volume volume1 = new Volume(100.0, VolumeUnit.CUBIC_CM);
        Volume volume2 = new Volume(100.0, VolumeUnit.CUBIC_CM);

        // when & then
        assertThat(volume1).isEqualTo(volume2);
        assertThat(volume1.hashCode()).isEqualTo(volume2.hashCode());
    }
}