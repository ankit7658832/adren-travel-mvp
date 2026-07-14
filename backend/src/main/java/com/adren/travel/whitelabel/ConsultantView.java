package com.adren.travel.whitelabel;

import java.time.Instant;
import java.util.UUID;

/**
 * A row in the Super Admin Console's Consultants list (PRD §21.6, FND-05) —
 * cross-module-safe, never the {@code Consultant} JPA entity itself
 * (RULES.md §1.4).
 */
public record ConsultantView(
    UUID consultantId,
    String businessName,
    Market homeMarket,
    ConsultantStatus status,
    Instant createdAt
) {
}
