package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.CurrencyMismatchException;
import com.commerce.product.domain.exception.InvalidMoneyException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode
public class Money implements ValueObject {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount) {
        this(amount, Currency.KRW);
    }

    public Money(BigDecimal amount, Currency currency) {
        validate(amount);
        this.amount = amount;
        this.currency = currency;
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    private void validate(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidMoneyException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException("Amount cannot be negative");
        }
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException("Subtraction result cannot be negative");
        }
        return new Money(result, this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException();
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}