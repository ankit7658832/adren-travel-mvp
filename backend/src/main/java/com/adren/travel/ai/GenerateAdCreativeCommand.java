package com.adren.travel.ai;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link AiApi#generateAdCreative} (PRD §14.4, AI-12). {@code
 * packageName}/{@code packageDescription}/{@code currentSellPrice} are the
 * REAL, caller-verified Package content (resolved by the {@code ads}
 * module via {@code BookingApi.findPackageById} — {@code ai} never reaches
 * into {@code booking}'s data itself, the same "caller supplies structured
 * grounding input" shape {@link GenerateItineraryCommand} already uses),
 * not something this module fetches or infers. Every generated variant is
 * validated against exactly these values — see {@code AiServiceImpl}'s
 * Javadoc for how.
 */
public record GenerateAdCreativeCommand(
    UUID consultantId,
    UUID packageId,
    String packageName,
    String packageDescription,
    Money currentSellPrice,
    int variantCount
) {

    public GenerateAdCreativeCommand {
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(packageId, "packageId must not be null");
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(packageDescription, "packageDescription must not be null");
        Objects.requireNonNull(currentSellPrice, "currentSellPrice must not be null");
        if (variantCount < 1) {
            throw new IllegalArgumentException("variantCount must be at least 1");
        }
    }
}
