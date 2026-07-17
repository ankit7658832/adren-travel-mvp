package com.adren.travel.ads;

import java.util.Objects;
import java.util.UUID;

/** Inputs to {@link AdsApi#generateAdCreativeForPackage} (PRD §14.4, AI-12). */
public record GenerateAdCreativeForPackageCommand(UUID packageId, int variantCount) {

    public GenerateAdCreativeForPackageCommand {
        Objects.requireNonNull(packageId, "packageId must not be null");
        if (variantCount < 1) {
            throw new IllegalArgumentException("variantCount must be at least 1");
        }
    }
}
