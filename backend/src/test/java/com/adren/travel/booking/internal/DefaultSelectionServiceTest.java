package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** PRD §9.2/§22.2's four-step ranking — one test per tie-break step (T2/T3). */
class DefaultSelectionServiceTest {

    private final DefaultSelectionService service = new DefaultSelectionService();

    private static SupplierSearchResult option(SupplierId supplierId, String rateId, long netRateAmount, Double rating) {
        return new SupplierSearchResult(supplierId, rateId, "Hotel " + rateId, "Standard",
            new Money(BigDecimal.valueOf(netRateAmount), CurrencyCode.INR), rating);
    }

    @Test
    void returnsEmptyWhenNoOptionsAreConfirmable() {
        assertThat(service.selectDefault(List.of(), null)).isEmpty();
    }

    @Test
    void selectsTheOnlyOptionWhenThereIsExactlyOne() {
        var only = option(SupplierId.HOTELBEDS, "r1", 5000, 4.0);

        assertThat(service.selectDefault(List.of(only), null)).contains(only);
    }

    @Test
    void t2SelectsThePreferredSupplierEvenWhenItIsNotTheCheapest() {
        var cheaper = option(SupplierId.HOTELBEDS, "cheap", 3000, 4.0);
        var preferred = option(SupplierId.STUBA, "preferred", 5000, 3.0);

        var result = service.selectDefault(List.of(cheaper, preferred), SupplierId.STUBA);

        assertThat(result).contains(preferred);
    }

    @Test
    void t3SelectsTheHighestMarginBestNetRateOptionWhenNoSupplierIsPreferred() {
        var expensive = option(SupplierId.HOTELBEDS, "expensive", 8000, 3.0);
        var cheapest = option(SupplierId.STUBA, "cheapest", 3000, 3.0);

        var result = service.selectDefault(List.of(expensive, cheapest), null);

        assertThat(result).contains(cheapest);
    }

    @Test
    void usesRatingAsTheFinalTiebreakerWhenMarginIsEqual() {
        var lowerRated = option(SupplierId.HOTELBEDS, "lower-rated", 5000, 3.0);
        var higherRated = option(SupplierId.STUBA, "higher-rated", 5000, 4.5);

        var result = service.selectDefault(List.of(lowerRated, higherRated), null);

        assertThat(result).contains(higherRated);
    }

    @Test
    void anOptionWithNoRatingLosesTheTiebreakToOneWithARating() {
        var noRating = option(SupplierId.HOTELBEDS, "no-rating", 5000, null);
        var rated = option(SupplierId.STUBA, "rated", 5000, 1.0);

        var result = service.selectDefault(List.of(noRating, rated), null);

        assertThat(result).contains(rated);
    }
}
