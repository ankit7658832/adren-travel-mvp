package com.adren.travel.booking.event;

import java.util.UUID;

/** Published when a Traveler Profile is created (PRD §20.10, BOK-14). */
public record TravelerProfileCreatedEvent(UUID travelerId, UUID consultantId) {
}
