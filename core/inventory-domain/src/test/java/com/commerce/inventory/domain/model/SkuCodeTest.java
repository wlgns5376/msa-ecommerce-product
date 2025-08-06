package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuCodeTest {

    @Test
    @DisplayName("유효한 코드로 SkuCode를 생성할 수 있다")
    void shouldCreateSkuCodeWithValidValue() {
        // given
        String value = "TSHIRT-BLACK-L";

        // when
        SkuCode skuCode = new SkuCode(value);

        // then
        assertThat(skuCode.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("null이거나 빈 값으로 SkuCode를 생성하면 예외가 발생한다")
    void shouldThrowExceptionWhenCreateWithInvalidValue(String invalidValue) {
        // when & then
        assertThatThrownBy(() -> new SkuCode(invalidValue))
                .isInstanceOf(InvalidSkuCodeException.class)
                .hasMessage("SKU 코드는 필수입니다");
    }

    @Test
    @DisplayName("같은 값을 가진 SkuCode는 동등하다")
    void shouldBeEqualWhenSameValue() {
        // given
        String value = "TSHIRT-BLACK-L";
        SkuCode skuCode1 = new SkuCode(value);
        SkuCode skuCode2 = new SkuCode(value);

        // when & then
        assertThat(skuCode1).isEqualTo(skuCode2);
        assertThat(skuCode1.hashCode()).isEqualTo(skuCode2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 SkuCode는 동등하지 않다")
    void shouldNotBeEqualWhenDifferentValue() {
        // given
        SkuCode skuCode1 = new SkuCode("TSHIRT-BLACK-L");
        SkuCode skuCode2 = new SkuCode("TSHIRT-BLACK-M");

        // when & then
        assertThat(skuCode1).isNotEqualTo(skuCode2);
    }
}