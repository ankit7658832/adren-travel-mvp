package com.adren.travel.booking;

import java.util.UUID;

/**
 * Thrown when an itinerary with an unapproved AI suggestion attempts to
 * become a Quotation (PRD §11.2 principle 3, AI-06) — a normal,
 * anticipated business-rule block, not an unexpected failure, so it's
 * mapped to 409 rather than surfacing as a generic error. Same shape as
 * {@link AtolDisclosureRequiredException}.
 */
public class AiApprovalRequiredException extends RuntimeException {

    public AiApprovalRequiredException(UUID itineraryId) {
        super("Itinerary " + itineraryId + " has an AI-generated suggestion pending approval — "
            + "a Consultant must explicitly approve it before this itinerary can become a Quotation (PRD §11.2)");
    }
}
