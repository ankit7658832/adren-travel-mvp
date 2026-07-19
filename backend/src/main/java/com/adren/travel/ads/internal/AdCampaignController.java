package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdCampaignCreativeVariantView;
import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ads.SubmitCampaignInputsCommand;
import com.adren.travel.ai.AdCreativeGenerationResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
