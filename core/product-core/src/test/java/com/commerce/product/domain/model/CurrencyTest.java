package com.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyTest {

    @Test
    @DisplayName("Currency enum은 KRW와 USD 값을 가진다")
    void shouldHaveKrwAndUsdCurrencies() {
        // When & Then
        assertThat(Currency.values()).hasSize(2);
        assertThat(Currency.valueOf("KRW")).isEqualTo(Currency.KRW);
        assertThat(Currency.valueOf("USD")).isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("KRW는 한국 원화를 나타낸다")
    void shouldRepresentKoreanWon() {
        // Given
        Currency currency = Currency.KRW;

        // When & Then
        assertThat(currency).isEqualTo(Currency.KRW);
        assertThat(currency.name()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("USD는 미국 달러를 나타낸다")
    void shouldRepresentUsDollar() {
        // Given
        Currency currency = Currency.USD;

        // When & Then
        assertThat(currency).isEqualTo(Currency.USD);
        assertThat(currency.name()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Currency는 문자열로 변환 가능하다")
    void shouldConvertToString() {
        // When & Then
        assertThat(Currency.KRW.toString()).isEqualTo("KRW");
        assertThat(Currency.USD.toString()).isEqualTo("USD");
    }
}