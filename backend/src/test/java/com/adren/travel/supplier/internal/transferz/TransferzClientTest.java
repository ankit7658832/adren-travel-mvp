package com.adren.travel.supplier.internal.transferz;

import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransferzClientTest {

    private final TransferzClient client = new TransferzClient(WebClient.builder());

    @Test
    void searchReturnsNormalizedResultsTaggedTransferz() {
        List<SupplierSearchResult> results = client.search("BOM Airport", "Hotel Taj", LocalDate.now());

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> assertThat(result.supplierId()).isEqualTo(SupplierId.TRANSFERZ));
    }

    @Test
    void noCoverageExceptionCarriesTheRoutePairInItsMessage() {
        TransferzClient.TransferzNoCoverageException exception =
            new TransferzClient.TransferzNoCoverageException("BOM Airport", "Hotel Taj");

        assertThat(exception.getMessage()).contains("BOM Airport").contains("Hotel Taj");
    }
}
