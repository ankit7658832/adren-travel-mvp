package com.adren.travel.whitelabel.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * doc/DESIGN.md §3.1 — {@code backgroundColor}/{@code textColorPrimary}/
 * {@code textColorSecondary} are the always-required flat fallback fields;
 * {@code logoUrl}/{@code backgroundImageUrl}/{@code domain} are optional.
 */
record UpdateBrandingRequest(
    String logoUrl,
    String backgroundImageUrl,
    @NotBlank String backgroundColor,
    @NotBlank String textColorPrimary,
    @NotBlank String textColorSecondary,
    String domain
) {
}
