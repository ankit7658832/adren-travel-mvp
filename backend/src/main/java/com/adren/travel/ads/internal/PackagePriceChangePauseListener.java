package com.adren.travel.ads.internal;

import com.adren.travel.ads.event.AdCampaignPausedEvent;
import com.adren.travel.booking.event.PackagePriceChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * ADS-12, PRD §23.5 Edge Case #11 — auto-pauses every Live campaign
 * promoting a Package the moment its price changes, until the Consultant
 * re-submits updated pricing/creative for a fresh policy review. Naturally
 * idempotent (RULES.md §2.2): {@link AdCampaignRepository#findByPackageIdAndStatus}
 * only ever returns campaigns still LIVE, so a retried delivery of the
 * same event (Modulith's at-least-once guarantee) simply finds nothing
 * left to pause on the second attempt — no dedup table needed, same
 * "state-check beats infrastructure idempotency" shape as {@code
 * AdCampaign#pause}'s own guard. No explicit {@code @Transactional} here —
 * {@code @ApplicationModuleListener} already composes {@code
 * @TransactionalEventListener}, which Spring forbids stacking a plain
 * {@code @Transactional} on top of; each {@code repository.save} call
 * below already runs in its own transaction via Spring Data's own
 * repository-level transactional proxy, same shape as {@code
 * StripePaymentConfirmationListener}'s identical omission.
 */
@Component
class PackagePriceChangePauseListener {

    private final AdCampaignRepository repository;
    private final ApplicationEventPublisher events;

    PackagePriceChangePauseListener(AdCampaignRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @ApplicationModuleListener
    void on(PackagePriceChangedEvent event) {
        for (AdCampaign campaign : repository.findByPackageIdAndStatus(event.packageId(), AdCampaignStatus.LIVE)) {
            campaign.pause();
            repository.save(campaign);
            events.publishEvent(new AdCampaignPausedEvent(campaign.getCampaignId(), campaign.getConsultantId()));
        }
    }
}
