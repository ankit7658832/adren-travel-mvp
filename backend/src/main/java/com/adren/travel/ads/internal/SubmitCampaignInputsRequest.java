package com.adren.travel.ads.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

record SubmitCampaignInputsRequest(
    @NotBlank String audienceDescription,
    @NotNull @Positive BigDecimal budgetCapAmount,
    @NotNull @Positive Integer durationDays
) {
}
