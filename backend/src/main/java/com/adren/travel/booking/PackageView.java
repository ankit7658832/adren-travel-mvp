package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A published Package visible to a Consultant's Users (PRD §20.7, BOK-12) — never the JPA entity itself. */
public record PackageView(
    UUID packageId,
    UUID sourceItineraryId,
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
