package com.adren.travel.ads.internal;

import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADS-09 — the mocked Meta Insights poller accrues onto every LIVE campaign, and only LIVE campaigns. */
@ExtendWith(MockitoExtension.class)
class AdCampaignPerformanceFeedServiceTest {

    @Mock
    AdCampaignRepository repository;

    @Mock
    MetaAdsClient metaAdsClient;

    @Test
    void pollPerformanceAccruesTheFetchedIncrementOntoEveryLiveCampaign() {
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), UUID.randomUUID(), CurrencyCode.INR);
        campaign.submitForPolicyReview();
        campaign.launch("meta-ref-123");
        when(repository.findByStatus(AdCampaignStatus.LIVE)).thenReturn(List.of(campaign));
        when(metaAdsClient.fetchPerformanceIncrement(campaignId))
            .thenReturn(new MetaAdsClient.PerformanceIncrement(120, 8, 1));

        new AdCampaignPerformanceFeedService(repository, metaAdsClient).pollPerformance();

        assertThat(campaign.getImpressions()).isEqualTo(120);
        assertThat(campaign.getClicks()).isEqualTo(8);
        assertThat(campaign.getBookingsAttributed()).isEqualTo(1);
        verify(repository).save(campaign);
    }

    @Test
    void pollPerformanceNeverFetchesForACampaignTheRepositoryDidNotReturnAsLive() {
        when(repository.findByStatus(AdCampaignStatus.LIVE)).thenReturn(List.of());

        new AdCampaignPerformanceFeedService(repository, metaAdsClient).pollPerformance();

        verify(metaAdsClient, org.mockito.Mockito.never()).fetchPerformanceIncrement(any());
    }
}
