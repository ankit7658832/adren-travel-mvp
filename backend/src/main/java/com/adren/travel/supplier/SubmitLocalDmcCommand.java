package com.adren.travel.supplier;

import java.util.List;
import java.util.Objects;

/**
 * Inputs to {@link SupplierSearchApi#submitLocalDmc} (PRD §10.3 step 1,
 * DMC-01). {@code consultantId} is resolved server-side from the calling
 * principal, never caller-supplied (RULES.md §5.2) — this command carries
 * only the business-facing submission fields.
 */
public record SubmitLocalDmcCommand(
    String businessName,
    List<String> productCategories,
    String sampleRatesSummary,
    String referencesInfo
) {

    public SubmitLocalDmcCommand {
        Objects.requireNonNull(businessName, "businessName must not be null");
        if (businessName.isBlank()) {
            throw new IllegalArgumentException("businessName must not be blank");
        }
        Objects.requireNonNull(productCategories, "productCategories must not be null");
        if (productCategories.isEmpty()) {
            throw new IllegalArgumentException("productCategories must not be empty");
        }
    }
}
