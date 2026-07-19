package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdCampaignCreativeVariantView;
import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.CampaignBillingDetailView;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ads.SubmitCampaignInputsCommand;
import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** ADS-02 — the campaign lifecycle's own resource root, distinct from {@code AdsController}'s package-scoped creative-generation endpoint. */
@RestController
@RequestMapping("/api/v1/campaigns")
class AdCampaignController {

    private final AdsApi adsApi;

    AdCampaignController(AdsApi adsApi) {
        this.adsApi = adsApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdCampaignView createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        return adsApi.createCampaign(new CreateCampaignCommand(request.packageId()));
    }

    /** ADS-03, PRD §14.2 steps 1-2. */
    @PostMapping("/{campaignId}/inputs")
    AdCampaignView submitCampaignInputs(@PathVariable UUID campaignId, @Valid @RequestBody SubmitCampaignInputsRequest request) {
        return adsApi.submitCampaignInputs(new SubmitCampaignInputsCommand(
            campaignId, request.audienceDescription(), request.budgetCapAmount(), request.durationDays()));
    }

    /** ADS-04, PRD §14.2 step 3. */
    @PostMapping("/{campaignId}/creative-variants")
    AdCreativeGenerationResult generateCreativeVariants(@PathVariable UUID campaignId,
                                                         @Valid @RequestBody GenerateCreativeVariantsRequest request) {
        return adsApi.generateCreativeForCampaign(campaignId, request.variantCount());
    }

    /** ADS-04 — the Campaign Builder's creative gallery reads persisted variants from here. */
    @GetMapping("/{campaignId}/creative-variants")
    List<AdCampaignCreativeVariantView> findCreativeVariants(@PathVariable UUID campaignId) {
        return adsApi.findCreativeVariantsForCampaign(campaignId);
    }

    /** ADS-05, PRD §14.2 step 4. */
    @PostMapping("/{campaignId}/creative-variants/{variantId}/approval")
    AdCampaignCreativeVariantView approveCreativeVariant(@PathVariable UUID campaignId, @PathVariable UUID variantId) {
        return adsApi.approveCreativeVariant(campaignId, variantId);
    }

    /**
     * ADS-06, PRD §14.2 step 5 — not literally named as its own sub-task
     * (only {@code /policy-review} is), but AC #1 needs a Consultant-facing
     * trigger into the review queue that no other story provides; see
     * {@code AdsApi#submitCampaignForPolicyReview}'s own Javadoc.
     */
    @PostMapping("/{campaignId}/submit-for-review")
    AdCampaignView submitCampaignForPolicyReview(@PathVariable UUID campaignId) {
        return adsApi.submitCampaignForPolicyReview(campaignId);
    }

    /** ADS-06, PRD §14.2 step 5 — Super Admin's brand-safety/policy review decision. */
    @PostMapping("/{campaignId}/policy-review")
    AdCampaignView rejectCampaignPolicyReview(@PathVariable UUID campaignId, @Valid @RequestBody PolicyReviewRejectionRequest request) {
        return adsApi.rejectCampaignPolicyReview(campaignId, request.reason());
    }

    /** ADS-06 — the Super Admin Console's brand-safety/policy review queue. */
    @GetMapping("/pending-policy-review")
    PageResponse<AdCampaignView> findCampaignsPendingPolicyReview(Pageable pageable) {
        return PageResponse.of(adsApi.findCampaignsPendingPolicyReview(pageable));
    }

    /** ADS-07, PRD §14.2 step 6. */
    @PostMapping("/{campaignId}/launch")
    AdCampaignView launchCampaign(@PathVariable UUID campaignId) {
        return adsApi.launchCampaign(campaignId);
    }

    /** ADS-09, PRD §14.2 step 7 — the Consultant Dashboard's Active Campaigns tab. */
    @GetMapping
    PageResponse<AdCampaignView> findCampaignsForConsultant(@RequestParam UUID consultantId, Pageable pageable) {
        return PageResponse.of(adsApi.findCampaignsForConsultant(consultantId, pageable));
    }

    /** ADS-11, PRD §14.3 — the Consultant-facing billing-transparency detail view. */
    @GetMapping("/{campaignId}/billing-detail")
    CampaignBillingDetailView findCampaignBillingDetail(@PathVariable UUID campaignId) {
        return adsApi.findCampaignBillingDetail(campaignId);
    }

    /** ADS-13 — a single campaign's current state, backing the suspension-status hook. */
    @GetMapping("/{campaignId}")
    AdCampaignView findCampaignById(@PathVariable UUID campaignId) {
        return adsApi.findCampaignById(campaignId);
    }
}
