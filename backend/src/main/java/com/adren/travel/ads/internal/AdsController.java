package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.GenerateAdCreativeForPackageCommand;
import com.adren.travel.ai.AdCreativeGenerationResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP surface for the Ads module (consumed by the React+Vite frontend).
 * Controllers stay thin — all logic lives in {@link AdsServiceImpl},
 * reached here only through the public {@link AdsApi}.
 */
@RestController
@RequestMapping("/api/v1/ads/packages")
class AdsController {

    private final AdsApi adsApi;

    AdsController(AdsApi adsApi) {
        this.adsApi = adsApi;
    }

    /** PRD §14.4, AI-12 — grounded ad-creative variants for a published Package, feeding the Campaign Builder's creative gallery (ADS-04). */
    @PostMapping("/{packageId}/ad-creative")
    AdCreativeGenerationResult generateAdCreativeForPackage(@PathVariable UUID packageId,
                                                             @Valid @RequestBody GenerateAdCreativeRequest request) {
        return adsApi.generateAdCreativeForPackage(
            new GenerateAdCreativeForPackageCommand(packageId, request.variantCount()));
    }
}
