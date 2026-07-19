package com.adren.travel.ads.internal;

import com.adren.travel.ads.event.AdCampaignPausedEvent;
import com.adren.travel.booking.event.PackagePriceChangedEvent;
import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADS-12 — the auto-pause listener only touches campaigns the repository reports as still LIVE, which is what makes it naturally idempotent on a retried delivery. */
@ExtendWith(MockitoExtension.class)
class PackagePriceChangePauseListenerTest {

    @Mock
    AdCampaignRepository repository;

    @Mock
    ApplicationEventPublisher events;

    @Test
    void onPausesEveryLiveCampaignPromotingTheChangedPackageAndPublishesAnEventPerCampaign() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(UUID.randomUUID(), packageId, consultantId, CurrencyCode.INR);
        campaign.submitForPolicyReview();
        campaign.launch("meta-ref-123");
        when(repository.findByPackageIdAndStatus(packageId, AdCampaignStatus.LIVE)).thenReturn(List.of(campaign));

        new PackagePriceChangePauseListener(repository, events).on(new PackagePriceChangedEvent(packageId, consultantId));

        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.PAUSED);
        verify(repository).save(campaign);
        ArgumentCaptor<AdCampaignPausedEvent> captor = ArgumentCaptor.forClass(AdCampaignPausedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().campaignId()).isEqualTo(campaign.getCampaignId());
    }

    @Test
    void onDoesNothingWhenNoCampaignForThatPackageIsCurrentlyLive() {
        UUID packageId = UUID.randomUUID();
        when(repository.findByPackageIdAndStatus(packageId, AdCampaignStatus.LIVE)).thenReturn(List.of());

        new PackagePriceChangePauseListener(repository, events).on(new PackagePriceChangedEvent(packageId, UUID.randomUUID()));

        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }
}
