package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Creates a Stripe PaymentIntent for a booking (PRD §12.4, §24.4, FIN-11).
 * Never carries card data — Stripe's hosted payment element captures the
 * card directly against the {@code clientSecret} this returns, so no raw
 * PAN ever has a field to reach the Adren backend through in the first
 * place (PRD §24.4's PCI-minimization NFR). {@code amount.currency()} may
 * be any of the six settlement currencies (PRD §12.2) — {@link
 * com.adren.travel.shared.CurrencyCode} already models exactly those six,
 * so no further per-currency validation is needed here.
 */
public record CreatePaymentIntentCommand(UUID bookingReferenceId, UUID consultantId, Money amount) {

    public CreatePaymentIntentCommand {
        Objects.requireNonNull(bookingReferenceId, "bookingReferenceId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
