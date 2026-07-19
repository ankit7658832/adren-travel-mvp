package com.adren.travel.ads;

import java.util.UUID;

/**
 * ADS-02 — {@code consultantId} is deliberately absent: resolved from the
 * Package itself (via {@code BookingApi.findPackageById}), same
 * never-trust-a-client-supplied-tenant-id shape {@link
 * GenerateAdCreativeForPackageCommand} already established for this
 * module.
 */
public record CreateCampaignCommand(UUID packageId) {
}
