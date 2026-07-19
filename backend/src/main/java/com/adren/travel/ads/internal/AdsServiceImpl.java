package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.GenerateAdCreativeForPackageCommand;
import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.GenerateAdCreativeCommand;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import com.adren.travel.shared.Money;
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

    AdsServiceImpl(BookingApi bookingApi, AiApi aiApi, AdAccountRepository adAccountRepository,
                   MetaAdsClient metaAdsClient) {
        this.bookingApi = bookingApi;
        this.aiApi = aiApi;
        this.adAccountRepository = adAccountRepository;
        this.metaAdsClient = metaAdsClient;
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
}
