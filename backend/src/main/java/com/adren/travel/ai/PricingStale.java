package com.adren.travel.ai;

/**
 * At least one approved line item's live supplier price/availability no
 * longer matches what was approved (PRD §11.3, AI-09, same "price changed,
 * please confirm" pattern as §10.2.4's Mystifly fare-expiry rule) — the
 * {@code booking} module blocks the booking confirmation rather than
 * silently honoring the stale price.
 */
public record PricingStale(String reason) implements AiPricingRevalidationResult {
}
