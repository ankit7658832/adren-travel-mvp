package com.adren.travel.ads.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    @Override
    public PerformanceIncrement fetchPerformanceIncrement(UUID campaignId) {
        int impressions = ThreadLocalRandom.current().nextInt(50, 200);
        int clicks = ThreadLocalRandom.current().nextInt(0, impressions / 10 + 1);
        int bookingsAttributed = ThreadLocalRandom.current().nextInt(0, clicks / 5 + 1);
        log.info("Meta ads stub: performance increment for campaignId={} impressions={} clicks={} bookingsAttributed={}",
            campaignId, impressions, clicks, bookingsAttributed);
        return new PerformanceIncrement(impressions, clicks, bookingsAttributed);
    }

    @Override
    public BigDecimal fetchSpendIncrement(UUID campaignId) {
        BigDecimal spendIncrement = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(10, 60))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        log.info("Meta ads stub: spend increment for campaignId={} amount={}", campaignId, spendIncrement);
        return spendIncrement;
    }
}
