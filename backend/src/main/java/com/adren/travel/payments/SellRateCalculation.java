package com.adren.travel.payments;

import com.adren.travel.shared.Money;

/**
 * The full net→buffer→markup→commission pipeline result for one line item
 * (PRD §12.1 Worked Examples A &amp; B, FIN-05) — every intermediate amount
 * is exposed, not just {@code sellRate}, so the ledger can show its work
 * (RULES.md §4.4).
 */
public record SellRateCalculation(
    Money netRate,
    FxRateSnapshot fxRateSnapshot,
    Money fxConvertedBase,
    Money bufferedAmount,
    Money markupAmount,
    Money sellRate,
    Money commissionAmount
) {
}
