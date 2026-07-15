package com.adren.travel.booking.event;

import java.util.UUID;

/** Published when a Package transitions DRAFT → PUBLISHED (PRD §9.1 Flow B step 3, §22.3, BOK-12). */
public record PackagePublishedEvent(UUID packageId, UUID sourceItineraryId, UUID consultantId, boolean promotedViaAds) {
}
