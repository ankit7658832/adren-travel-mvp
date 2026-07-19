package com.adren.travel.ads.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record GenerateCreativeVariantsRequest(@Min(1) @NotNull Integer variantCount) {
}
