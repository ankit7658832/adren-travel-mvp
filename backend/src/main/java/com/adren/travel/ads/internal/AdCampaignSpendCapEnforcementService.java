package com.adren.travel.ads.internal;

import com.adren.travel.ads.event.AdCampaignSpendCapReachedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Near-real-time spend-cap enforcement (PRD §14.3, §24.6's NFR, ADS-10) —
 * polls the same {@link MetaAdsClient} seam as {@link
 * AdCampaignPerformanceFeedService} but on its own, tighter cadence, since
 * §24.6 only requires near-real-time enforcement for spend, not for
 * impressions/clicks display. {@link AdCampaign#recordSpend} owns the
 * actual cap-and-transition guard; this service is only responsible for
 * feeding it real increments and publishing the transition event when it
 * happens.
 */
@Service
class AdCampaignSpendCapEnforcementService {

    private final AdCampaignRepository repository;
    private final AdCampaignSpendTransactionRepository spendTransactionRepository;
    private final MetaAdsClient metaAdsClient;
    private final ApplicationEventPublisher events;

    AdCampaignSpendCapEnforcementService(AdCampaignRepository repository,
                                          AdCampaignSpendTransactionRepository spendTransactionRepository,
                                          MetaAdsClient metaAdsClient, ApplicationEventPublisher events) {
        this.repository = repository;
        this.spendTransactionRepository = spendTransactionRepository;
        this.metaAdsClient = metaAdsClient;
        this.events = events;
    }

    @Scheduled(cron = "${adren.ads.spend-cap-enforcement-cron:0 */2 * * * *}")
    @Transactional
    void enforceSpendCaps() {
        for (AdCampaign campaign : repository.findByStatus(AdCampaignStatus.LIVE)) {
            BigDecimal increment = metaAdsClient.fetchSpendIncrement(campaign.getCampaignId());
            campaign.recordSpend(increment);
            repository.save(campaign);
            // ADS-11's billing-transparency AC needs the per-transaction
            // breakdown, not just the running total recordSpend maintains —
            // one row per poll increment, the actual amount fetched (not
            // truncated to whatever recordSpend capped spend_to_date at).
            spendTransactionRepository.save(
                new AdCampaignSpendTransaction(UUID.randomUUID(), campaign.getCampaignId(), increment));

            if (campaign.getStatus() == AdCampaignStatus.SPEND_CAP_REACHED) {
                events.publishEvent(new AdCampaignSpendCapReachedEvent(
                    campaign.getCampaignId(), campaign.getConsultantId(), campaign.getSpendToDateAmount()));
            }
        }
    }
}
