package com.adren.travel.ads.internal;

import com.adren.travel.ads.event.AdCampaignSpendCapReachedEvent;
import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADS-10 — the spend-cap enforcement poller publishes an event only on the run that actually crosses the threshold. */
@ExtendWith(MockitoExtension.class)
class AdCampaignSpendCapEnforcementServiceTest {

    @Mock
    AdCampaignRepository repository;

    @Mock
    AdCampaignSpendTransactionRepository spendTransactionRepository;

    @Mock
    MetaAdsClient metaAdsClient;

    @Mock
    ApplicationEventPublisher events;

    private AdCampaign liveCampaign(UUID campaignId, UUID consultantId, String budgetCapAmount) {
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        campaign.submitCampaignInputs("Adults 25-45", new BigDecimal(budgetCapAmount), 14);
        campaign.submitForPolicyReview();
        campaign.launch("meta-ref-123");
        return campaign;
    }

    @Test
    void enforceSpendCapsAccruesTheIncrementAndDoesNotPublishWhenStillBelowTheCap() {
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = liveCampaign(campaignId, UUID.randomUUID(), "500.00");
        when(repository.findByStatus(AdCampaignStatus.LIVE)).thenReturn(List.of(campaign));
        when(metaAdsClient.fetchSpendIncrement(campaignId)).thenReturn(new BigDecimal("50.00"));

        new AdCampaignSpendCapEnforcementService(repository, spendTransactionRepository, metaAdsClient, events)
            .enforceSpendCaps();

        assertThat(campaign.getSpendToDateAmount()).isEqualByComparingTo("50.00");
        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.LIVE);
        verify(repository).save(campaign);
        verify(events, never()).publishEvent(any());

        ArgumentCaptor<AdCampaignSpendTransaction> txnCaptor = ArgumentCaptor.forClass(AdCampaignSpendTransaction.class);
        verify(spendTransactionRepository).save(txnCaptor.capture());
        assertThat(txnCaptor.getValue().getCampaignId()).isEqualTo(campaignId);
        assertThat(txnCaptor.getValue().getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void enforceSpendCapsPublishesTheRealEventTheRunTheCapIsCrossed() {
        UUID campaignId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        AdCampaign campaign = liveCampaign(campaignId, consultantId, "500.00");
        campaign.recordSpend(new BigDecimal("480.00"));
        when(repository.findByStatus(AdCampaignStatus.LIVE)).thenReturn(List.of(campaign));
        when(metaAdsClient.fetchSpendIncrement(campaignId)).thenReturn(new BigDecimal("50.00"));

        new AdCampaignSpendCapEnforcementService(repository, spendTransactionRepository, metaAdsClient, events)
            .enforceSpendCaps();

        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.SPEND_CAP_REACHED);
        ArgumentCaptor<AdCampaignSpendCapReachedEvent> captor = ArgumentCaptor.forClass(AdCampaignSpendCapReachedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().campaignId()).isEqualTo(campaignId);
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().spendToDateAmount()).isEqualByComparingTo("500.00");
    }
}
