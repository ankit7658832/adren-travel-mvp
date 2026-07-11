package com.adren.travel.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test — no Spring context, no database, runs in milliseconds.
 * This is the default test type in this codebase (see the
 * {@code testing-strategy} skill): reach for {@code @SpringBootTest} or
 * Testcontainers only when the thing under test genuinely needs a Spring
 * context or a real database.
 */
class MoneyTest {

    @Test
    void appliesPercentMarkupCorrectly() {
        // PRD Section 12.1 worked example: net 10,000 + 15% markup = 11,500
        Money net = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);

        Money sellRate = net.applyMarkupPercent(BigDecimal.valueOf(15));

        assertThat(sellRate.amount()).isEqualByComparingTo("11500.00");
        assertThat(sellRate.currency()).isEqualTo(CurrencyCode.INR);
    }

    @Test
    void rejectsMixedCurrencyAddition() {
        Money inr = Money.zero(CurrencyCode.INR);
        Money aud = Money.zero(CurrencyCode.AUD);

        assertThatThrownBy(() -> inr.plus(aud))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Currency mismatch");
    }

    @Test
    void roundsToTwoDecimalPlaces() {
        Money money = new Money(BigDecimal.valueOf(10.005), CurrencyCode.USD);

        assertThat(money.amount()).isEqualByComparingTo("10.01");
    }
}
