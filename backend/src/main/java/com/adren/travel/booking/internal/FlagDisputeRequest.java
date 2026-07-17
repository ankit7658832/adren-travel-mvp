package com.adren.travel.booking.internal;

import jakarta.validation.constraints.NotBlank;

record FlagDisputeRequest(@NotBlank String reason) {
}
