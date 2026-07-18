package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdsApi;
import com.adren.travel.ads.GenerateAdCreativeForPackageCommand;
import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.GenerateAdCreativeCommand;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import com.adren.travel.shared.Money;
import org.springframework.stereotype.Service;

/**
 * Internal implementation of {@link AdsApi} (PRD §14, AI-12). Not visible
 * outside this module.
 * <p>
 * This is the module's first real content beyond its {@code package-info}
 * scaffold — only what AI-12 needs (ad-creative generation grounded in a
 * Package's real content). The full Ads Campaign Builder epic (ADS-*
 * stories — Meta account provisioning, campaign lifecycle, spend-cap
 * enforcement) is separate, out of scope here.
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

    AdsServiceImpl(BookingApi bookingApi, AiApi aiApi) {
        this.bookingApi = bookingApi;
        this.aiApi = aiApi;
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
}
