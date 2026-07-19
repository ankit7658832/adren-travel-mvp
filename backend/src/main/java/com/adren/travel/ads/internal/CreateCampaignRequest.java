package com.adren.travel.ads.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record CreateCampaignRequest(@NotNull UUID packageId) {
}
