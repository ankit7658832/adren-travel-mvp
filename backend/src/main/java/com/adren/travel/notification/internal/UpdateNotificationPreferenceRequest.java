package com.adren.travel.notification.internal;

import jakarta.validation.constraints.NotBlank;

record UpdateNotificationPreferenceRequest(@NotBlank String secondaryChannel) {
}
