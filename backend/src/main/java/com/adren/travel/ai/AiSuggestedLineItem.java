package com.adren.travel.ai;

import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;

import java.time.Instant;

/**
 * One line item an AI generation call suggested (PRD §11.3, AI-04) — always
 * carries {@code supplierId} and {@code availabilityAsOf} as first-class
 * fields, not something the frontend has to separately fetch or infer, per
 * PRD §11.3's acceptance criterion ("every line item shows supplier source
 * and live availability before approval"). Every field here traces back to
 * a real {@code SupplierSearchResult} the generation call was grounded
 * against — see {@code AiServiceImpl}'s Javadoc for how that's enforced,
 * not just requested via prompt.
 */
public record AiSuggestedLineItem(
    SupplierId supplierId,
    String supplierRateId,
    String propertyName,
    String roomType,
    Money netRate,
    Instant availabilityAsOf
) {
}
