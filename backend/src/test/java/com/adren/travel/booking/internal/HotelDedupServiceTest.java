package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HotelDedupServiceTest {

    private final HotelDedupService service = new HotelDedupService();

    @Test
    void mergesTheSamePropertyOfferedByTwoSuppliersKeepingTheLowerNetRateBOK20() {
        SupplierSearchResult hotelbeds = new SupplierSearchResult(SupplierId.HOTELBEDS, "hb-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.0);
        SupplierSearchResult stuba = new SupplierSearchResult(SupplierId.STUBA, "st-1", "Taj Palace",
            "Standard Room", new Money(BigDecimal.valueOf(4800), CurrencyCode.INR), 4.2);

        List<SupplierSearchResult> deduped = service.deduplicate(List.of(hotelbeds, stuba));

        assertThat(deduped).hasSize(1);
        assertThat(deduped.get(0).supplierId()).isEqualTo(SupplierId.STUBA); // lower net rate wins
    }

    @Test
    void mergesPropertiesWhoseNamesDifferOnlyByFormattingBOK20() {
        SupplierSearchResult hotelbeds = new SupplierSearchResult(SupplierId.HOTELBEDS, "hb-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.0);
        SupplierSearchResult tbo = new SupplierSearchResult(SupplierId.TBO, "tbo-1", "Taj  Palace,",
            "Deluxe Room", new Money(BigDecimal.valueOf(5100), CurrencyCode.INR), 4.0);

        List<SupplierSearchResult> deduped = service.deduplicate(List.of(hotelbeds, tbo));

        assertThat(deduped).hasSize(1);
    }

    @Test
    void doesNotMergeGenuinelyDistinctProperties() {
        SupplierSearchResult hotelA = new SupplierSearchResult(SupplierId.HOTELBEDS, "hb-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.0);
        SupplierSearchResult hotelB = new SupplierSearchResult(SupplierId.STUBA, "st-1", "Oberoi Grand",
            "Standard Room", new Money(BigDecimal.valueOf(4800), CurrencyCode.INR), 4.2);

        List<SupplierSearchResult> deduped = service.deduplicate(List.of(hotelA, hotelB));

        assertThat(deduped).hasSize(2);
    }

    @Test
    void breaksATieOnEqualNetRateByRatingBOK20() {
        SupplierSearchResult lowerRating = new SupplierSearchResult(SupplierId.HOTELBEDS, "hb-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 3.5);
        SupplierSearchResult higherRating = new SupplierSearchResult(SupplierId.STUBA, "st-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.5);

        List<SupplierSearchResult> deduped = service.deduplicate(List.of(lowerRating, higherRating));

        assertThat(deduped).hasSize(1);
        assertThat(deduped.get(0).supplierId()).isEqualTo(SupplierId.STUBA);
    }

    @Test
    void doesNotCompareNetRatesAcrossDifferentCurrencies() {
        SupplierSearchResult inr = new SupplierSearchResult(SupplierId.HOTELBEDS, "hb-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), null);
        SupplierSearchResult aed = new SupplierSearchResult(SupplierId.BYOS, "byos-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(50), CurrencyCode.AED), null);

        List<SupplierSearchResult> deduped = service.deduplicate(List.of(inr, aed));

        // Currencies differ so net rate isn't a valid comparison basis here
        // (BOK-17's own FX-conversion machinery, not this method's job) —
        // still collapses to one entry (same property), keeping the first seen.
        assertThat(deduped).hasSize(1);
        assertThat(deduped.get(0).supplierId()).isEqualTo(SupplierId.HOTELBEDS);
    }
}
