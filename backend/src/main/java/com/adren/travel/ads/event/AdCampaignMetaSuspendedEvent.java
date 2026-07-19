package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-13, PRD §23.5 Edge Case #12 / §25 T17 — a campaign was flagged "suspended — action required" following a mocked Meta ad-account suspension signal. */
public record AdCampaignMetaSuspendedEvent(UUID campaignId, UUID consultantId) {
}
