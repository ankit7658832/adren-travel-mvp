package com.adren.travel.ads;

import com.adren.travel.ai.AdCreativeGenerationResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Public API of the Ads/Campaign Management module. Other modules must
 * depend on this interface, never on classes under {@code
 * com.adren.travel.ads.internal}.
 */
public interface AdsApi {

    /**
     * Generates grounded ad-creative variants for a published Package
     * (PRD §14.4, AI-12) — resolves the Package's REAL, live content via
     * {@code BookingApi.findPackageById} and passes it to {@code
     * AiApi.generateAdCreative} as caller-verified grounding input (this
     * module never trusts a client-supplied name/price). Same "Create
     * package" role/capability-grant shape as {@code
     * BookingApi#publishPackage} — generating creative for a package is
     * part of the same authority as owning it.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    AdCreativeGenerationResult generateAdCreativeForPackage(GenerateAdCreativeForPackageCommand command);
}
