package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.CurrencyMismatchException;
import com.commerce.product.domain.exception.InvalidMoneyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("유효한 금액과 통화로 Money를 생성할 수 있다")
    void shouldCreateMoneyWithValidAmount() {
        // Given
        BigDecimal amount = new BigDecimal("10000");
        Currency currency = Currency.KRW;

        // When
        Money money = new Money(amount, currency);

        // Then
        assertThat(money).isNotNull();
        assertThat(money.getAmount()).isEqualByComparingTo(amount);
        assertThat(money.getCurrency()).isEqualTo(currency);
    }

    @Test
    @DisplayName("통화를 지정하지 않으면 기본값 KRW로 생성된다")
    void shouldUseDefaultCurrencyWhenNotSpecified() {
        // Given
        BigDecimal amount = new BigDecimal("5000");

        // When
        Money money = new Money(amount);

        // Then
        assertThat(money.getCurrency()).isEqualTo(Currency.KRW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-100", "-0.01"})
    @DisplayName("음수 금액으로 Money 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenAmountIsNegative(String negativeAmount) {
        // Given
        BigDecimal amount = new BigDecimal(negativeAmount);

        // When & Then
        assertThatThrownBy(() -> new Money(amount))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("Amount cannot be negative");
    }

    @Test
    @DisplayName("null 금액으로 Money 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenAmountIsNull() {
        // When & Then
        assertThatThrownBy(() -> new Money(null))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("0원 Money를 생성할 수 있다")
    void shouldCreateZeroMoney() {
        // When
        Money money = Money.zero();

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(money.getCurrency()).isEqualTo(Currency.KRW);
    }

    @Test
    @DisplayName("같은 통화의 Money를 더할 수 있다")
    void shouldAddMoneyWithSameCurrency() {
        // Given
        Money money1 = new Money(new BigDecimal("1000"));
        Money money2 = new Money(new BigDecimal("2000"));

        // When
        Money result = money1.add(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(result.getCurrency()).isEqualTo(Currency.KRW);
    }

    @Test
    @DisplayName("다른 통화의 Money를 더하면 예외가 발생한다")
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        // Given
        Money krwMoney = new Money(new BigDecimal("1000"), Currency.KRW);
        Money usdMoney = new Money(new BigDecimal("10"), Currency.USD);

        // When & Then
        assertThatThrownBy(() -> krwMoney.add(usdMoney))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("Cannot operate on different currencies");
    }

    @Test
    @DisplayName("Money를 곱할 수 있다")
    void shouldMultiplyMoney() {
        // Given
        Money money = new Money(new BigDecimal("1000"));
        int multiplier = 3;

        // When
        Money result = money.multiply(multiplier);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(result.getCurrency()).isEqualTo(Currency.KRW);
    }

    @Test
    @DisplayName("Money를 뺄 수 있다")
    void shouldSubtractMoney() {
        // Given
        Money money1 = new Money(new BigDecimal("5000"));
        Money money2 = new Money(new BigDecimal("2000"));

        // When
        Money result = money1.subtract(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("뺄셈 결과가 음수면 예외가 발생한다")
    void shouldThrowExceptionWhenSubtractionResultIsNegative() {
        // Given
        Money money1 = new Money(new BigDecimal("1000"));
        Money money2 = new Money(new BigDecimal("2000"));

        // When & Then
        assertThatThrownBy(() -> money1.subtract(money2))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("Subtraction result cannot be negative");
    }

    @Test
    @DisplayName("동일한 금액과 통화를 가진 Money는 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingEqualMoney() {
        // Given
        Money money1 = new Money(new BigDecimal("1000.00"));
        Money money2 = new Money(new BigDecimal("1000.00"));

        // When & Then
        assertThat(money1).isEqualTo(money2);
        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }

    @Test
    @DisplayName("다른 금액을 가진 Money는 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingDifferentAmounts() {
        // Given
        Money money1 = new Money(new BigDecimal("1000"));
        Money money2 = new Money(new BigDecimal("2000"));

        // When & Then
        assertThat(money1).isNotEqualTo(money2);
    }

    @Test
    @DisplayName("Money 크기를 비교할 수 있다")
    void shouldCompareMoney() {
        // Given
        Money money1 = new Money(new BigDecimal("1000"));
        Money money2 = new Money(new BigDecimal("2000"));

        // When & Then
        assertThat(money1.isLessThan(money2)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isTrue();
        assertThat(money1.isGreaterThanOrEqual(money1)).isTrue();
    }

    @Test
    @DisplayName("toString 메서드는 금액과 통화를 포함한 문자열을 반환한다")
    void shouldReturnFormattedStringWhenCallingToString() {
        // Given
        Money money = new Money(new BigDecimal("12345.67"), Currency.KRW);

        // When
        String result = money.toString();

        // Then
        assertThat(result).isEqualTo("12345.67 KRW");
    }
}