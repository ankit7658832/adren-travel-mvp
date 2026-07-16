package com.adren.travel.supplier.internal.stuba;

import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StubaClientTest {

    private final StubaClient client = new StubaClient(WebClient.builder());

    @Test
    void searchReturnsNormalizedResultsTaggedStuba() {
        List<SupplierSearchResult> results = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3));

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> assertThat(result.supplierId()).isEqualTo(SupplierId.STUBA));
        assertThat(results.get(0).supplierRateId()).isNotBlank();
        assertThat(results.get(0).netRate()).isNotNull();
    }
}
