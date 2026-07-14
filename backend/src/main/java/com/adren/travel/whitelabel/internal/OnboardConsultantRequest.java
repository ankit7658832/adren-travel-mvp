package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.Market;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

record OnboardConsultantRequest(
    @NotBlank String businessName,
    @NotNull Market homeMarket,
    Map<String, String> kycFields
) {
}
