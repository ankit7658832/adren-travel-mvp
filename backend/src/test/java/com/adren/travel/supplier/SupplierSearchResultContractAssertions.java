package com.adren.travel.supplier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-08 — shared contract assertions every supplier client's mapped
 * {@link SupplierSearchResult} output must satisfy (PRD §20.2-20.6's
 * normalized-field discipline, backend-best-practices skill §6). Applying
 * this to a new supplier client's test (Phase 2's {@code SUP-*} epic) is
 * the enforcement mechanism this story's acceptance criterion asks for —
 * a supplier-specific raw field name (TBO's {@code TraceId}/{@code
 * ResultIndex}, Hotelbeds' {@code rateKey}/{@code hotelCode}) leaking into
 * a normalized display field fails here, not silently in production.
 */
public final class SupplierSearchResultContractAssertions {

    /**
     * Raw supplier API field names that must never appear as literal text
     * in a normalized display field — each is mapped to its proper
     * normalized field (e.g. TBO's {@code ResultIndex} -> {@code
     * supplierRateId}) instead, per PRD §10.2's per-supplier field-mapping
     * tables. Extend this list as new supplier clients are added.
     */
    private static final List<String> RAW_SUPPLIER_FIELD_NAMES = List.of(
        "TraceId", "ResultIndex", "rateKey", "hotelCode", "HotelCode", "DayRates");

    private SupplierSearchResultContractAssertions() {
    }

    public static void assertConformsToNormalizedContract(List<SupplierSearchResult> results) {
        assertThat(results).as("supplier search results").isNotEmpty();
        results.forEach(SupplierSearchResultContractAssertions::assertConformsToNormalizedContract);
    }

    public static void assertConformsToNormalizedContract(SupplierSearchResult result) {
        assertThat(result.supplierId()).as("supplierId").isNotNull();
        assertThat(result.supplierRateId()).as("supplierRateId").isNotBlank();
        assertThat(result.propertyName()).as("propertyName").isNotBlank();
        assertThat(result.roomType()).as("roomType").isNotBlank();

        assertThat(result.netRate()).as("netRate").isNotNull();
        assertThat(result.netRate().amount()).as("netRate.amount").isGreaterThan(BigDecimal.ZERO);
        assertThat(result.netRate().currency()).as("netRate.currency").isNotNull();

        if (result.rating() != null) {
            assertThat(result.rating()).as("rating").isBetween(0.0, 5.0);
        }

        assertNoRawSupplierFieldNameLeaked(result);
    }

    private static void assertNoRawSupplierFieldNameLeaked(SupplierSearchResult result) {
        String displayText = result.propertyName() + " " + result.roomType() + " " + result.supplierRateId();
        for (String rawFieldName : RAW_SUPPLIER_FIELD_NAMES) {
            assertThat(displayText)
                .as("propertyName/roomType/supplierRateId must not leak the raw supplier field name '%s'", rawFieldName)
                .doesNotContain(rawFieldName);
        }
    }
}
