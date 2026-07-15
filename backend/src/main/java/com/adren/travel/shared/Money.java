package com.adren.travel.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable money value object. Always use {@link BigDecimal} for monetary
 * amounts (PRD Section 9.6 / 24.1: "decimal-safe arithmetic... no
 * floating-point rounding errors given multi-currency markup stacking").
 * Never represent money as {@code double} or {@code float} anywhere in this
 * codebase.
 */
public record Money(BigDecimal amount, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), currency);
    }

    /** Applies a percentage markup, e.g. markupPercent = 15 for 15% (PRD Section 12.1). */
    public Money applyMarkupPercent(BigDecimal markupPercent) {
        BigDecimal factor = BigDecimal.ONE.add(markupPercent.movePointLeft(2));
        return multiply(factor);
    }

    /**
     * The amount that {@code percent}% of this Money represents, e.g.
     * {@code percentOf(5)} for a 5% Adren commission on the supplier net
     * rate (PRD Section 12.1, FIN-02) — unlike {@link #applyMarkupPercent},
     * this returns just the percentage slice, not the original plus it.
     */
    public Money percentOf(BigDecimal percent) {
        return multiply(percent.movePointLeft(2));
    }

    /**
     * Converts this amount into {@code targetCurrency} at {@code rate}
     * (this amount times rate) — the FX layer this class's own
     * currency-mismatch guard tells callers to use before combining
     * cross-currency amounts (PRD Section 12.2, FIN-05).
     */
    public Money convertTo(CurrencyCode targetCurrency, BigDecimal rate) {
        return new Money(this.amount.multiply(rate), targetCurrency);
    }

    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                "Currency mismatch: %s vs %s — convert via the FX layer before combining amounts (PRD 12.2)"
                    .formatted(this.currency, other.currency));
        }
    }
}
