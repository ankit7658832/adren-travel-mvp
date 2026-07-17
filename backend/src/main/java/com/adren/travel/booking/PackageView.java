package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A published Package visible to a Consultant's Users (PRD §20.7, BOK-12)
 * — never the JPA entity itself. {@code consultantId} (added for AI-12) is
 * the package's OWNING consultant — needed by cross-module callers like
 * {@code ads.AdsServiceImpl} that must ground an AI call in the true
 * owner, not necessarily the calling principal (who may be a SUPER_ADMIN
 * acting on the owner's behalf).
 */
public record PackageView(
    UUID packageId,
    UUID sourceItineraryId,
    UUID consultantId,
    String name,
    String description,
    LocalDate validityStart,
    LocalDate validityEnd,
    BigDecimal basePrice,
    BigDecimal markupPrice,
    CurrencyCode currency,
    int maxPax,
    boolean promotedViaAds
) {
}
