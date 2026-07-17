package com.adren.travel.booking.internal;

import jakarta.validation.constraints.Positive;

record UpdateActivityHeadcountRequest(@Positive int headcount) {
}
