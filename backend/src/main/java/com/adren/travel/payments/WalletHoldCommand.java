package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared input shape for {@link PaymentsApi#placeHold}, {@link
 * PaymentsApi#resolveHoldAsDebit} and {@link PaymentsApi#resolveHoldAsRelease}
 * (PRD §12.3, FIN-07) — all three take the same (bookingId, consultantId,
 * amount) triple, so one command type covers all three rather than three
 * near-identical records.
 */
public record WalletHoldCommand(UUID bookingId, UUID consultantId, Money amount) {

    public WalletHoldCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
