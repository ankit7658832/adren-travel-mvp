package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ads.AdCampaignCreativeVariantView;
import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.CampaignBillingDetailView;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ads.GenerateAdCreativeForPackageCommand;
import com.adren.travel.ads.SpendTransactionView;
import com.adren.travel.ads.SubmitCampaignInputsCommand;
import com.adren.travel.ads.event.AdCampaignCreatedEvent;
import com.adren.travel.ads.event.AdCampaignCreativeVariantApprovedEvent;
import com.adren.travel.ads.event.AdCampaignInputsSubmittedEvent;
import com.adren.travel.ads.event.AdCampaignLaunchedEvent;
import com.adren.travel.ads.event.AdCampaignPolicyReviewRejectedEvent;
import com.adren.travel.ads.event.AdCampaignSubmittedForPolicyReviewEvent;
import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.ai.AdCreativeSuggestion;
import com.adren.travel.ai.AdCreativeVariant;
import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.GenerateAdCreativeCommand;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final AdCampaignCreativeVariantRepository creativeVariantRepository;
    private final AdCampaignSpendTransactionRepository spendTransactionRepository;
    private final ApplicationEventPublisher events;

    AdsServiceImpl(BookingApi bookingApi, AiApi aiApi, AdAccountRepository adAccountRepository,
                   MetaAdsClient metaAdsClient, AdCampaignRepository adCampaignRepository,
                   AdCampaignCreativeVariantRepository creativeVariantRepository,
                   AdCampaignSpendTransactionRepository spendTransactionRepository, ApplicationEventPublisher events) {
        this.bookingApi = bookingApi;
        this.aiApi = aiApi;
        this.adAccountRepository = adAccountRepository;
        this.metaAdsClient = metaAdsClient;
        this.adCampaignRepository = adCampaignRepository;
        this.creativeVariantRepository = creativeVariantRepository;
        this.spendTransactionRepository = spendTransactionRepository;
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

    @Override
    @Transactional
    public AdCampaignView submitCampaignInputs(SubmitCampaignInputsCommand command) {
        AdCampaign campaign = adCampaignRepository.findById(command.campaignId())
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + command.campaignId()));
        CurrentPrincipal.resolveTenantScope(campaign.getConsultantId());

        campaign.submitCampaignInputs(command.audienceDescription(), command.budgetCapAmount(), command.durationDays());
        adCampaignRepository.save(campaign);
        events.publishEvent(new AdCampaignInputsSubmittedEvent(campaign.getCampaignId(),
            campaign.getAudienceDescription(), campaign.getBudgetCapAmount(), campaign.getDurationDays()));

        return toView(campaign);
    }

    @Override
    @Transactional
    public AdCreativeGenerationResult generateCreativeForCampaign(UUID campaignId, int variantCount) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));
        CurrentPrincipal.resolveTenantScope(campaign.getConsultantId());

        AdCreativeGenerationResult result = generateAdCreativeForPackage(
            new GenerateAdCreativeForPackageCommand(campaign.getPackageId(), variantCount));

        // AI-05: NoViableAdCreative is a legitimate outcome, not an error —
        // nothing to persist, the caller sees the explicit failure state.
        if (result instanceof AdCreativeSuggestion suggestion) {
            for (AdCreativeVariant variant : suggestion.variants()) {
                creativeVariantRepository.save(new AdCampaignCreativeVariant(
                    UUID.randomUUID(), campaignId, variant.headline(), variant.bodyText(), null));
            }
        }
        return result;
    }

    @Override
    public List<AdCampaignCreativeVariantView> findCreativeVariantsForCampaign(UUID campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));
        CurrentPrincipal.resolveTenantScope(campaign.getConsultantId());

        return creativeVariantRepository.findByCampaignId(campaignId).stream()
            .map(v -> new AdCampaignCreativeVariantView(
                v.getVariantId(), v.getCampaignId(), v.getHeadline(), v.getBodyText(), v.getImageRef(), v.isApproved()))
            .toList();
    }

    @Override
    @Transactional
    public AdCampaignCreativeVariantView approveCreativeVariant(UUID campaignId, UUID variantId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));
        CurrentPrincipal.resolveTenantScope(campaign.getConsultantId());

        AdCampaignCreativeVariant variant = creativeVariantRepository.findById(variantId)
            .orElseThrow(() -> new IllegalArgumentException("No creative variant: " + variantId));
        if (!variant.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Variant " + variantId + " does not belong to campaign " + campaignId);
        }

        variant.approve();
        creativeVariantRepository.save(variant);
        events.publishEvent(new AdCampaignCreativeVariantApprovedEvent(campaignId, variantId));

        return new AdCampaignCreativeVariantView(variant.getVariantId(), variant.getCampaignId(),
            variant.getHeadline(), variant.getBodyText(), variant.getImageRef(), variant.isApproved());
    }

    @Override
    @Transactional
    public AdCampaignView submitCampaignForPolicyReview(UUID campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));
        CurrentPrincipal.resolveTenantScope(campaign.getConsultantId());

        // ADS-05's own AC, enforced here as the actual gate on submission —
        // service-layer business rule check, entity-owned status guard,
        // same split BookingServiceImpl#saveAsQuotation established for
        // "at least one line item."
        List<AdCampaignCreativeVariant> variants = creativeVariantRepository.findByCampaignId(campaignId);
        if (variants.isEmpty() || variants.stream().anyMatch(v -> !v.isApproved())) {
            throw new IllegalStateException(
                "Campaign " + campaignId + " cannot be submitted for policy review: every creative variant must be approved first");
        }

        campaign.submitForPolicyReview();
        adCampaignRepository.save(campaign);
        events.publishEvent(new AdCampaignSubmittedForPolicyReviewEvent(campaign.getCampaignId(), campaign.getConsultantId()));

        return toView(campaign);
    }

    @Override
    @Transactional
    public AdCampaignView rejectCampaignPolicyReview(UUID campaignId, String reason) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));

        campaign.rejectPolicyReview(reason);
        adCampaignRepository.save(campaign);
        events.publishEvent(
            new AdCampaignPolicyReviewRejectedEvent(campaign.getCampaignId(), campaign.getConsultantId(), reason));

        return toView(campaign);
    }

    @Override
    public Page<AdCampaignView> findCampaignsPendingPolicyReview(Pageable pageable) {
        return adCampaignRepository.findByStatus(AdCampaignStatus.PENDING_POLICY_REVIEW, pageable)
            .map(AdsServiceImpl::toView);
    }

    @Override
    @Transactional
    public AdCampaignView launchCampaign(UUID campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));

        // Validate eligibility BEFORE calling out to the (mocked) Meta API —
        // campaign.launch() itself guards this same precondition, but only
        // after the external call would already have happened; checking
        // here first means an ineligible campaign never reaches the launch
        // call at all, not just that its result gets discarded.
        if (campaign.getStatus() != AdCampaignStatus.PENDING_POLICY_REVIEW) {
            throw new IllegalStateException(
                "Only a PENDING_POLICY_REVIEW campaign can launch, was: " + campaign.getStatus());
        }

        String metaCampaignRef = metaAdsClient.launchCampaign(campaignId);
        campaign.launch(metaCampaignRef);
        adCampaignRepository.save(campaign);
        events.publishEvent(
            new AdCampaignLaunchedEvent(campaign.getCampaignId(), campaign.getConsultantId(), metaCampaignRef));

        return toView(campaign);
    }

    @Override
    public Page<AdCampaignView> findCampaignsForConsultant(UUID consultantId, Pageable pageable) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return adCampaignRepository.findByConsultantId(scopedConsultantId, pageable)
            .map(AdsServiceImpl::toView);
    }

    @Override
    public CampaignBillingDetailView findCampaignBillingDetail(UUID campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("No campaign: " + campaignId));
        CurrentPrincipal.resolveTenantScope(campaign.getConsultantId());

        List<SpendTransactionView> transactions = spendTransactionRepository
            .findByCampaignIdOrderByRecordedAtDesc(campaignId).stream()
            .map(t -> new SpendTransactionView(t.getTransactionId(), t.getAmount(), t.getRecordedAt()))
            .toList();

        return new CampaignBillingDetailView(campaign.getCampaignId(), campaign.getSpendToDateAmount(),
            campaign.getBudgetCapAmount(), campaign.getBudgetCapCurrency(), transactions);
    }

    private static AdCampaignView toView(AdCampaign campaign) {
        return new AdCampaignView(campaign.getCampaignId(), campaign.getPackageId(), campaign.getConsultantId(),
            campaign.getStatus().name(), campaign.getAudienceDescription(), campaign.getBudgetCapAmount(),
            campaign.getBudgetCapCurrency(), campaign.getDurationDays(), campaign.getMetaCampaignRef(),
            campaign.getSpendToDateAmount(), campaign.getRejectionReason(), campaign.getImpressions(),
            campaign.getClicks(), campaign.getBookingsAttributed());
    }
}
