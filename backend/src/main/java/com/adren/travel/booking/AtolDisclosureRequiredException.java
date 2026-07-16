package com.adren.travel.booking;

import java.util.UUID;

/**
 * Thrown when a UK Consultant attempts to publish a dynamic flight+hotel
 * Package before completing the ATOL disclosure step (PRD §17.2, §22.3 T5,
 * BOK-11) — a normal, anticipated business-rule block, not an unexpected
 * failure, so it's mapped to 409 rather than surfacing as a generic error.
 */
public class AtolDisclosureRequiredException extends RuntimeException {

    public AtolDisclosureRequiredException(UUID packageId) {
        super("Package " + packageId + " requires ATOL disclosure completion before it can be published "
            + "(UK dynamic flight+hotel combo, PRD §17.2)");
    }
}
