package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#calculateAdSpendBilling} (PRD §1, §19,
 * ADS-14). {@code spendAmount} is the ad-spend increment being billed —
 * in the Consultant's own settlement currency, per {@code Money}'s own
 * self-validating currency-pairing rule (RULES.md §4.4).
 */
public record CalculateAdSpendBillingCommand(UUID campaignId, UUID consultantId, Money spendAmount) {

    public CalculateAdSpendBillingCommand {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(spendAmount, "spendAmount must not be null");
    }
}
