package com.adren.travel.supplier.internal.hotelbeds;

import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static com.adren.travel.supplier.SupplierSearchResultContractAssertions.assertConformsToNormalizedContract;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-08 — the reference usage of {@code SupplierSearchResultContractAssertions}:
 * every supplier client's test should apply the shared contract check
 * rather than hand-rolling field-by-field assertions per client.
 */
class HotelbedsClientTest {

    private final HotelbedsClient client = new HotelbedsClient(WebClient.builder());

    @Test
    void searchResultsConformToTheNormalizedSupplierSearchResultContract() {
        List<SupplierSearchResult> results = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), "any-credential");

        assertConformsToNormalizedContract(results);
    }

    @Test
    void searchReturnsResultsTaggedHotelbeds() {
        List<SupplierSearchResult> results = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), "any-credential");

        assertThat(results).allSatisfy(result -> assertThat(result.supplierId()).isEqualTo(SupplierId.HOTELBEDS));
    }
}
