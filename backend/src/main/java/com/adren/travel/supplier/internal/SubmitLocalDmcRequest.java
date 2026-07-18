package com.adren.travel.supplier.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

record SubmitLocalDmcRequest(
    @NotBlank String businessName,
    @NotEmpty List<String> productCategories,
    String sampleRatesSummary,
    String referencesInfo
) {
}
