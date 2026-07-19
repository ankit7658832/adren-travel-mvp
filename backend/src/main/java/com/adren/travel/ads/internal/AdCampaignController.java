package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ads.SubmitCampaignInputsCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
