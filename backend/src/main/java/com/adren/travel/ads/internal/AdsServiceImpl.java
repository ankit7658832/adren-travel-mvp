package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ads.GenerateAdCreativeForPackageCommand;
import com.adren.travel.ads.event.AdCampaignCreatedEvent;
import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.GenerateAdCreativeCommand;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Internal implementation of {@link AdsApi} (PRD §14). Not visible outside
 * this module.
 * <p>
 * Resolves the Package's REAL, live content via {@link
 * BookingApi#findPackageById} and passes it into {@link
 * AiApi#generateAdCreative} as caller-verified grounding input — this
 * module never trusts a client-supplied name/price, same "grounded
 * generation only" principle {@code AiServiceImpl} enforces server-side
 * for itinerary suggestions.
 */
@Service
class AdsServiceImpl implements AdsApi {

    private final BookingApi bookingApi;
    private final AiApi aiApi;
    private final AdAccountRepository adAccountRepository;
    private final MetaAdsClient metaAdsClient;
    private final AdCampaignRepository adCampaignRepository;
    private final ApplicationEventPublisher events;

    AdsServiceImpl(BookingApi bookingApi, AiApi aiApi, AdAccountRepository adAccountRepository,
                   MetaAdsClient metaAdsClient, AdCampaignRepository adCampaignRepository,
                   ApplicationEventPublisher events) {
        this.bookingApi = bookingApi;
        this.aiApi = aiApi;
        this.adAccountRepository = adAccountRepository;
        this.metaAdsClient = metaAdsClient;
        this.adCampaignRepository = adCampaignRepository;
        this.events = events;
    }

    @Override
    public AdCreativeGenerationResult generateAdCreativeForPackage(GenerateAdCreativeForPackageCommand command) {
        PackageView travelPackage = bookingApi.findPackageById(command.packageId());
        Money currentSellPrice = new Money(travelPackage.basePrice().add(travelPackage.markupPrice()),
            travelPackage.currency());

        return aiApi.generateAdCreative(new GenerateAdCreativeCommand(travelPackage.consultantId(),
            travelPackage.packageId(), travelPackage.name(), travelPackage.description(), currentSellPrice,
            command.variantCount()));
    }

    @Override
    @Transactional
    public AdAccountView provisionAdAccount(UUID consultantId) {
        AdAccount account = adAccountRepository.findByConsultantId(consultantId)
            .orElseGet(() -> {
                String metaBusinessManagerId = metaAdsClient.provisionAdAccount(consultantId);
                AdAccount created = new AdAccount(UUID.randomUUID(), consultantId, metaBusinessManagerId);
                return adAccountRepository.save(created);
            });
        return toView(account);
    }

    private static AdAccountView toView(AdAccount account) {
        return new AdAccountView(account.getAdAccountId(), account.getConsultantId(),
            account.getMetaBusinessManagerId(), account.getProvisionedAt());
    }

    @Override
    @Transactional
    public AdCampaignView createCampaign(CreateCampaignCommand command) {
        // findPackageById already enforces PUBLISHED status and tenant
        // scoping (RULES.md §5.2) — no need to duplicate either check here.
        PackageView travelPackage = bookingApi.findPackageById(command.packageId());

        AdCampaign campaign = new AdCampaign(UUID.randomUUID(), travelPackage.packageId(),
            travelPackage.consultantId(), travelPackage.currency());
        adCampaignRepository.save(campaign);
        events.publishEvent(
            new AdCampaignCreatedEvent(campaign.getCampaignId(), campaign.getPackageId(), campaign.getConsultantId()));

        return toView(campaign);
    }

    private static AdCampaignView toView(AdCampaign campaign) {
        return new AdCampaignView(campaign.getCampaignId(), campaign.getPackageId(), campaign.getConsultantId(),
            campaign.getStatus().name(), campaign.getAudienceDescription(), campaign.getBudgetCapAmount(),
            campaign.getBudgetCapCurrency(), campaign.getDurationDays(), campaign.getMetaCampaignRef(),
            campaign.getSpendToDateAmount(), campaign.getRejectionReason());
    }
}
