package com.adren.travel.ads.internal;

import jakarta.validation.constraints.NotBlank;

record PolicyReviewRejectionRequest(@NotBlank String reason) {
}
