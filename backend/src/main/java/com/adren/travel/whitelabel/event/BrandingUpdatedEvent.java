package com.adren.travel.whitelabel.event;

import java.util.UUID;

/**
 * Published when a Consultant's branding is saved (PRD §13.2, FND-06).
 * {@code domain} is carried directly (not just {@code consultantId}) since
 * FND-07's live-storefront cache invalidation and FND-08's dynamic CORS
 * allow-list both key off the domain value itself, not the consultantId.
 */
public record BrandingUpdatedEvent(UUID consultantId, String domain) {
}
