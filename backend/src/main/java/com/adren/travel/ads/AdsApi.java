package com.adren.travel.ads;

import com.adren.travel.ai.AdCreativeGenerationResult;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

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

    /**
     * Provisions an Adren-managed Meta ad account/Business Manager for a
     * Consultant (PRD §14.1, ADS-01) — Super Admin only, per PRD §6's
     * "No (executes)" row (a Consultant never owns their own Meta account).
     * Idempotent-shaped: a second call for an already-provisioned
     * Consultant returns the existing account rather than erroring or
     * duplicating.
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    AdAccountView provisionAdAccount(UUID consultantId);

    /**
     * Creates a new campaign in PendingApproval for a published Package
     * (PRD §14.2 steps 1-2, ADS-02) — same "Create package" authority
     * shape as {@link #generateAdCreativeForPackage}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    AdCampaignView createCampaign(CreateCampaignCommand command);

    /**
     * Sets a PENDING_APPROVAL campaign's audience/budget/duration inputs
     * (PRD §14.2 steps 1-2, ADS-03) — same authority shape as {@link
     * #createCampaign}; tenant-scoped to the campaign's own owning
     * Consultant inside the implementation (RULES.md §5.2), since the
     * campaign already exists by this point and the caller is never
     * trusted to supply their own consultantId.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    AdCampaignView submitCampaignInputs(SubmitCampaignInputsCommand command);

    /**
     * Generates grounded ad-creative variants for a campaign's own Package
     * (PRD §14.2 step 3, ADS-04) and persists each surviving variant
     * against the campaign (PRD §20.13's {@code creative_variants[]}) —
     * reuses {@link #generateAdCreativeForPackage}'s own grounding, never
     * re-deriving it. A {@link com.adren.travel.ai.NoViableAdCreative}
     * result is a legitimate outcome (AI-05's explicit-failure-state
     * principle) that persists nothing, not an error.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    AdCreativeGenerationResult generateCreativeForCampaign(UUID campaignId, int variantCount);

    /** The persisted creative variants for a campaign (PRD §20.13), tenant-scoped to the campaign's owning Consultant. */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    List<AdCampaignCreativeVariantView> findCreativeVariantsForCampaign(UUID campaignId);
}
