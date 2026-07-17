package com.adren.travel.payments;

import com.adren.travel.shared.Money;

/**
 * Result of {@link PaymentsApi#calculateIndiaGstTcs} (PRD §12.1 Worked
 * Example C, FIN-17). {@code applied} is false whenever {@code
 * adren.payments.tax.india.enabled} is off (the default — see {@code
 * IndiaTaxProperties}'s Javadoc for why) — in that case {@code
 * gstAmount}/{@code tcsAmount} are both zero, distinguishing "this
 * calculation ran but nothing was owed" from "this ran with illustrative,
 * unconfirmed rates," which callers must be able to tell apart before
 * this ever reaches a real invoice.
 */
public record IndiaGstTcsCalculation(Money gstAmount, Money tcsAmount, boolean applied) {
}
