package com.adren.travel.booking.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

record UpdatePackagePriceRequest(@NotNull @Positive BigDecimal markupPrice) {
}
