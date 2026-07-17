package com.adren.travel.payments;

import com.adren.travel.shared.Money;

/**
 * Result of {@link PaymentsApi#calculateUkTomsVat} (PRD §12.1 Worked
 * Example D, FIN-18). {@code applied} is false whenever {@code
 * adren.payments.tax.uk-toms.enabled} is off (the default — see {@code
 * UkTomsVatProperties}'s Javadoc), matching {@code
 * IndiaGstTcsCalculation}'s same "never silently charge an unconfirmed
 * illustrative rate" shape.
 */
public record UkTomsVatCalculation(Money vatAmount, boolean applied) {
}
