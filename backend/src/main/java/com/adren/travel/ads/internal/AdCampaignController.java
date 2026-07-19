package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.CreateCampaignCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
