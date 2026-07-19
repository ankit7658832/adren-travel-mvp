package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ads.AdsApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** ADS-01 — same "one controller per module owning a slice of /consultants/{id}/..." shape as {@code ByosCredentialController}/{@code NotificationPreferenceController}. */
@RestController
@RequestMapping("/api/v1/consultants/{consultantId}/ad-account")
class AdAccountController {

    private final AdsApi adsApi;

    AdAccountController(AdsApi adsApi) {
        this.adsApi = adsApi;
    }

    @PostMapping
    AdAccountView provisionAdAccount(@PathVariable UUID consultantId) {
        return adsApi.provisionAdAccount(consultantId);
    }

    /** ADS-13, PRD §23.5 Edge Case #12 / §25 T17 — the mocked Meta ad-account suspension signal. */
    @PostMapping("/suspension")
    void reportSuspension(@PathVariable UUID consultantId) {
        adsApi.reportMetaAccountSuspension(consultantId);
    }
}
