package com.adren.travel.ads.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Mock-phase-only {@link MetaAdsClient} — logs and returns a synthetic identifier rather than calling a real provider, matching FIN-11's {@code StripeClient} stubbing precedent. */
@Component
class StubMetaAdsClient implements MetaAdsClient {

    private static final Logger log = LoggerFactory.getLogger(StubMetaAdsClient.class);

    @Override
    public String provisionAdAccount(UUID consultantId) {
        String metaBusinessManagerId = "stub-bm-" + UUID.randomUUID();
        log.info("Meta ads stub: provisioned Business Manager {} for consultant={}", metaBusinessManagerId, consultantId);
        return metaBusinessManagerId;
    }

    @Override
    public String launchCampaign(UUID campaignId) {
        String metaCampaignRef = "stub-campaign-" + UUID.randomUUID();
        log.info("Meta ads stub: launched campaign ref={} for campaignId={}", metaCampaignRef, campaignId);
        return metaCampaignRef;
    }
}
