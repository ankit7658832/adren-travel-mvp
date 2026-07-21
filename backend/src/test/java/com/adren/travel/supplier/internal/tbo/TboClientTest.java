package com.adren.travel.supplier.internal.tbo;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

import static com.adren.travel.supplier.SupplierSearchResultContractAssertions.assertConformsToNormalizedContract;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TboClientTest {

    private final TboClient client = new TboClient(WebClient.builder());

    @Test
    @Tag("supplier-sandbox-fixture")
    void searchReturnsNormalizedResultsTaggedTbo() {
        TboClient.TboSearchResponse response = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), null);

        assertThat(response.results()).allSatisfy(result -> assertThat(result.supplierId()).isEqualTo(SupplierId.TBO));
        // TST-08 — same shared contract check HotelbedsClientTest applies;
        // proves the harness isn't Hotelbeds-specific.
        assertConformsToNormalizedContract(response.results());
    }

    @Test
    @Tag("supplier-sandbox-fixture")
    void searchGeneratesANewTraceIdWhenNoneSupplied() {
        TboClient.TboSearchResponse response = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), null);

        assertThat(response.traceId()).isNotBlank();
    }

    @Test
    @Tag("supplier-sandbox-fixture")
    void searchReusesTheSuppliedTraceIdAcrossTheSameSearchSession() {
        String existingTraceId = "existing-trace-id-123";

        TboClient.TboSearchResponse response = client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3), existingTraceId);

        assertThat(response.traceId()).isEqualTo(existingTraceId);
    }

    /**
     * TST-06, PRD S23.2 Edge Case #4 / S25 T19 — sandbox and production TBO
     * environments are documented to behave differently around session
     * expiry; the three tests above only ever exercised the always-succeeds
     * (sandbox-shaped) path. This is the production-shaped counterpart: a
     * TraceId expiring mid-build must force a full re-search, never a
     * silent failure or a partial retry with the stale TraceId.
     */
    @Test
    @Tag("supplier-production-fixture")
    void searchThrowsTboTraceIdExpiredExceptionWhenTheSuppliedTraceIdHasExpiredT19() {
        assertThatThrownBy(() -> client.search("BOM", LocalDate.now(), LocalDate.now().plusDays(3),
            TboClient.PRODUCTION_FIXTURE_EXPIRED_TRACE_ID))
            .isInstanceOf(TboClient.TboTraceIdExpiredException.class)
            .hasMessageContaining(TboClient.PRODUCTION_FIXTURE_EXPIRED_TRACE_ID);
    }
}
