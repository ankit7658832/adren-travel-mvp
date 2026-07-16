package com.adren.travel.supplier.internal.widgety;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WidgetyClientTest {

    private final WidgetyClient client = new WidgetyClient(WebClient.builder());

    @Test
    void searchReturnsNormalizedResultsTaggedWidgety() {
        List<WidgetyClient.WidgetySearchResult> results = client.search("Miami", LocalDate.now().plusMonths(1));

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> assertThat(result.normalized().supplierId()).isEqualTo(SupplierId.WIDGETY));
    }

    @Test
    void searchFlattensMultiPortDetailAsMetadataNotSeparateLineItems() {
        List<WidgetyClient.WidgetySearchResult> results = client.search("Miami", LocalDate.now().plusMonths(1));

        assertThat(results.get(0).ports()).isNotEmpty();
    }
}
