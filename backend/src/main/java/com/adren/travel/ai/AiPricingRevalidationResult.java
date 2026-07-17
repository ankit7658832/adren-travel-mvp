package com.adren.travel.ai;

/**
 * Outcome of {@link AiApi#revalidateAiPricingAtBooking} (PRD §11.3, AI-09)
 * — modeled as an explicit typed result rather than an exception
 * (backend-best-practices §7's "model failure as a first-class response
 * state" principle, same shape as {@link AiItineraryGenerationResult}),
 * since "the live price moved" is an expected outcome of re-checking
 * supplier data at booking time, not a programming error. The
 * {@code booking} module converts a {@link PricingStale} result into its
 * own domain exception at its own boundary (mirroring how it already
 * handles {@code InventoryNoLongerAvailableException}) — {@code ai} itself
 * never throws for this outcome.
 */
public sealed interface AiPricingRevalidationResult permits PricingConfirmed, PricingStale {
}
