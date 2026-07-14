package com.adren.travel.whitelabel;

import java.time.Instant;
import java.util.UUID;

/** A Consultant's current branding (PRD §13.2, FND-06) — cross-module-safe, never the JPA entity itself. */
public record BrandingProfileView(
    UUID consultantId,
    String logoUrl,
    String backgroundImageUrl,
    String backgroundColor,
    String textColorPrimary,
    String textColorSecondary,
    String domain,
    Instant updatedAt
) {
}
