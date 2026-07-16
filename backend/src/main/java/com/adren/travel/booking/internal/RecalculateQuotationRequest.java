package com.adren.travel.booking.internal;

import jakarta.validation.constraints.Positive;

record RecalculateQuotationRequest(@Positive int travelerCount) {
}
