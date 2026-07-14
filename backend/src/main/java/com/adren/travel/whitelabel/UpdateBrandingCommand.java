package com.adren.travel.whitelabel;

import java.util.UUID;

/**
 * Cross-module-safe input to {@link WhitelabelApi#updateBranding} (PRD
 * §13.2, FND-06) — a plain value, never a JPA entity (RULES.md §1.4).
 * {@code backgroundColor}/{@code textColorPrimary}/{@code textColorSecondary}
 * are the always-required flat fallback fields (doc/DESIGN.md §3.1/§3.4);
 * {@code logoUrl}/{@code backgroundImageUrl}/{@code domain} are optional.
 */
public record UpdateBrandingCommand(
    UUID consultantId,
    String logoUrl,
    String backgroundImageUrl,
    String backgroundColor,
    String textColorPrimary,
    String textColorSecondary,
    String domain
) {
}
