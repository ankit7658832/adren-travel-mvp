package com.adren.travel.ads;

import java.util.UUID;

/** One persisted creative variant (PRD §20.13) — cross-module-safe, never the JPA entity itself. */
public record AdCampaignCreativeVariantView(
    UUID variantId, UUID campaignId, String headline, String bodyText, String imageRef, boolean approved) {
}
