package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A reusable, sellable Package converted from a Quotation (PRD §20.7,
 * §9.1 Flow B, BOK-10) — package-private, own table. Named
 * {@code TravelPackage} rather than PRD's own "Package" to avoid shadowing
 * {@code java.lang.Package} within this file. {@code sourceItineraryId} is
 * a real FK constraint (see the migration) since {@code Itinerary} is
 * owned by this SAME module. {@code basePrice} is auto-filled server-side
 * from the source itinerary's priced line items; {@code markupPrice} is
 * the Consultant-editable amount added on top, sharing {@code currency}
 * with {@code basePrice} (RULES.md §4.4: an amount is never auditable
 * without its currency).
 */
@Entity
@Table(name = "travel_package")
class TravelPackage {

    @Id
    private UUID packageId;

    private UUID sourceItineraryId;
    private UUID consultantId;
    private String name;
    private String description;
    private LocalDate validityStart;
    private LocalDate validityEnd;
    private BigDecimal basePrice;
    private BigDecimal markupPrice;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    private int maxPax;
    private boolean promotedViaAds;
    private UUID adCampaignId;
    private boolean dynamicFlightHotelCombo;

    @Enumerated(EnumType.STRING)
    private PackageStatus status;

    private Instant createdAt;

    protected TravelPackage() {
        // JPA
    }

    TravelPackage(UUID packageId, UUID sourceItineraryId, UUID consultantId, String name, String description,
                  LocalDate validityStart, LocalDate validityEnd, BigDecimal basePrice, BigDecimal markupPrice,
                  CurrencyCode currency, int maxPax) {
        this.packageId = packageId;
        this.sourceItineraryId = sourceItineraryId;
        this.consultantId = consultantId;
        this.name = name;
        this.description = description;
        this.validityStart = validityStart;
        this.validityEnd = validityEnd;
        this.basePrice = basePrice;
        this.markupPrice = markupPrice;
        this.currency = currency;
        this.maxPax = maxPax;
        this.promotedViaAds = false;
        this.adCampaignId = null;
        this.dynamicFlightHotelCombo = false;
        this.status = PackageStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    /**
     * DRAFT → PUBLISHED (PRD §9.1 Flow B step 3, §22.3, BOK-12) — makes the
     * package visible to the Consultant's Users. {@code promoteViaAds}
     * records whether the Consultant opted into Meta campaign promotion
     * (PRD §20.7's {@code promoted_via_ads}); the actual hand-off into the
     * Ads Campaign Builder (ADS-03) is a frontend navigation concern, not
     * modeled here. The UK ATOL disclosure gate (BOK-11) is deferred: this
     * vertical slice has no Flight line item type yet, so {@code
     * dynamicFlightHotelCombo} can never actually be true and a gate
     * checking it would be unreachable, untestable code.
     */
    void publish(boolean promoteViaAds) {
        if (status == PackageStatus.PUBLISHED) {
            throw new IllegalStateException("Package " + packageId + " is already PUBLISHED");
        }
        this.status = PackageStatus.PUBLISHED;
        this.promotedViaAds = promoteViaAds;
    }

    UUID getPackageId() {
        return packageId;
    }

    UUID getSourceItineraryId() {
        return sourceItineraryId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getName() {
        return name;
    }

    LocalDate getValidityStart() {
        return validityStart;
    }

    LocalDate getValidityEnd() {
        return validityEnd;
    }

    BigDecimal getBasePrice() {
        return basePrice;
    }

    BigDecimal getMarkupPrice() {
        return markupPrice;
    }

    CurrencyCode getCurrency() {
        return currency;
    }

    int getMaxPax() {
        return maxPax;
    }

    boolean isDynamicFlightHotelCombo() {
        return dynamicFlightHotelCombo;
    }

    boolean isPromotedViaAds() {
        return promotedViaAds;
    }

    String getDescription() {
        return description;
    }

    PackageStatus getStatus() {
        return status;
    }
}
