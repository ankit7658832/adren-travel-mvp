package com.adren.travel.ai;

/** Every approved line item's live supplier price still matches what was approved — booking may proceed. */
public record PricingConfirmed() implements AiPricingRevalidationResult {
}
