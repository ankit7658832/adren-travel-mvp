package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Consolidates an itinerary's line items — potentially priced in more than
 * one sell currency (e.g. a BYOS supplier configured with a different
 * settlement currency than the Consultant's other suppliers) — into one
 * total in {@code targetSellCurrency} (PRD §23.1 Edge Case #2, BOK-17).
 * <p>
 * {@code ratesToTargetCurrency} maps every non-target sell currency present
 * among the itinerary's line items to the rate that converts it into {@code
 * targetSellCurrency} — supplied by the caller, mirroring {@link
 * com.adren.travel.payments.SnapshotFxRateCommand}'s own "rate supplied by
 * caller, an FX rate source/provider is outside this story's scope"
 * pattern (FIN-04). A currency present on a line item with no matching
 * entry here fails loudly rather than silently mixing currencies.
 */
public record ConsolidateCheckoutTotalCommand(
    UUID itineraryId,
    CurrencyCode targetSellCurrency,
    Map<CurrencyCode, BigDecimal> ratesToTargetCurrency
) {

    public ConsolidateCheckoutTotalCommand {
        Objects.requireNonNull(itineraryId, "itineraryId must not be null");
        Objects.requireNonNull(targetSellCurrency, "targetSellCurrency must not be null");
        ratesToTargetCurrency = ratesToTargetCurrency != null ? Map.copyOf(ratesToTargetCurrency) : Map.of();
    }
}
