package com.adren.travel.booking.event;

import java.util.UUID;

/** Published when a Quotation is converted into a Package (PRD §9.1 Flow B, §20.7, BOK-10). */
public record PackageCreatedEvent(UUID packageId, UUID sourceItineraryId, UUID consultantId) {
}
