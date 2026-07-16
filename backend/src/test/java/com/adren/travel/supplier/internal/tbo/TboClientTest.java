package com.adren.travel.supplier.internal.tbo;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TboClientTest {

    private final TboClient client = new TboClient(WebClient.builder());

    @Test
    void searchReturnsNormalizedResultsTaggedTbo() {
        TboClient.TboSearchResponse response = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), null);

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results()).allSatisfy(result -> assertThat(result.supplierId()).isEqualTo(SupplierId.TBO));
    }

    @Test
    void searchGeneratesANewTraceIdWhenNoneSupplied() {
        TboClient.TboSearchResponse response = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), null);

        assertThat(response.traceId()).isNotBlank();
    }

    @Test
    void searchReusesTheSuppliedTraceIdAcrossTheSameSearchSession() {
        String existingTraceId = "existing-trace-id-123";

        TboClient.TboSearchResponse response = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), existingTraceId);

        assertThat(response.traceId()).isEqualTo(existingTraceId);
    }
}
