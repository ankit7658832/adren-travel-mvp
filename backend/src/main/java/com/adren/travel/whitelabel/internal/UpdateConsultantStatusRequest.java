package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.ConsultantStatus;
import jakarta.validation.constraints.NotNull;

record UpdateConsultantStatusRequest(@NotNull ConsultantStatus status) {
}
