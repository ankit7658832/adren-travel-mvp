package com.adren.travel.payments;

import com.adren.travel.shared.Money;

/**
 * Result of {@link PaymentsApi#calculateAdSpendBilling} (PRD §1, §19,
 * ADS-14) — {@code feeAmount} is zero and {@code applied} is false
 * whenever {@code adren.payments.ad-spend.enabled} is off (the default),
 * same "never silently charge an unconfirmed illustrative rate" shape as
 * {@code UkTomsVatCalculation}/{@code IndiaGstTcsCalculation}.
 */
public record AdSpendBillingCalculation(Money spendAmount, Money feeAmount, boolean applied) {
}
