package com.adren.travel.ads.internal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mocked Meta Insights poll (PRD §14.2 step 7, §20.13's {@code
 * performance_snapshot}, ADS-09) — mirrors {@code
 * SupplierContentStalenessCheckService}'s scheduled-poll shape. Every LIVE
 * campaign's impressions/clicks/bookings_attributed are incremented each
 * run via {@link MetaAdsClient#fetchPerformanceIncrement}; Phase 2's real
 * Meta Insights API integration swaps in behind the same seam.
 */
@Service
class AdCampaignPerformanceFeedService {

    private final AdCampaignRepository repository;
    private final MetaAdsClient metaAdsClient;

    AdCampaignPerformanceFeedService(AdCampaignRepository repository, MetaAdsClient metaAdsClient) {
        this.repository = repository;
        this.metaAdsClient = metaAdsClient;
    }

    @Scheduled(cron = "${adren.ads.performance-feed-cron:0 */5 * * * *}")
    @Transactional
    void pollPerformance() {
        for (AdCampaign campaign : repository.findByStatus(AdCampaignStatus.LIVE)) {
            MetaAdsClient.PerformanceIncrement increment = metaAdsClient.fetchPerformanceIncrement(campaign.getCampaignId());
            campaign.recordPerformanceSnapshot(increment.impressions(), increment.clicks(), increment.bookingsAttributed());
            repository.save(campaign);
        }
    }
}
