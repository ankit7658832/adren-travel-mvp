package com.adren.travel.ads.internal;

/** PRD §20.13's Ad Campaign status enum. */
enum AdCampaignStatus {
    PENDING_APPROVAL,
    PENDING_POLICY_REVIEW,
    LIVE,
    PAUSED,
    REJECTED,
    SPEND_CAP_REACHED
}
