package com.adren.travel.booking;

import java.util.UUID;

/**
 * Thrown when {@code AiApi.revalidateAiPricingAtBooking} finds that an
 * AI-approved line item's live supplier price/availability no longer
 * matches what was approved (PRD §11.3, AI-09) — a normal, anticipated
 * business-rule block (the same "price changed, please confirm" pattern
 * §10.2.4 requires for Mystifly fare expiry), not an unexpected failure,
 * so it's mapped to 409 rather than surfacing as a generic error. Same
 * shape as {@link AiApprovalRequiredException}.
 */
public class AiPricingStaleException extends RuntimeException {

    public AiPricingStaleException(UUID itineraryId, String reason) {
        super("Itinerary " + itineraryId + " cannot be booked: " + reason);
    }
}
