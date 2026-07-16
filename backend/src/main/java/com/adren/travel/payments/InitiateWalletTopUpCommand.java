package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Starts a wallet top-up via Stripe (PRD §23.4 Edge Case #10, FIN-15) — a
 * PaymentIntent is created immediately, but {@code availableBalance} is not
 * credited until the confirming webhook actually arrives (see {@link
 * PaymentsApi#initiateWalletTopUp}'s Javadoc). Distinct from {@link
 * CreatePaymentIntentCommand} (which settles a booking) since a top-up has
 * no {@code bookingReferenceId} to carry.
 */
public record InitiateWalletTopUpCommand(UUID consultantId, Money amount) {

    public InitiateWalletTopUpCommand {
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
