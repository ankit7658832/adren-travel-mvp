package com.adren.travel.ads;

import java.time.Instant;
import java.util.UUID;

/** A Consultant's provisioned Meta ad account (PRD §14.1, ADS-01) — cross-module-safe, never the JPA entity itself. */
public record AdAccountView(UUID adAccountId, UUID consultantId, String metaBusinessManagerId, Instant provisionedAt) {
}
