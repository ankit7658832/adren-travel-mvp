package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-12, PRD §23.5 Edge Case #11 — a Live campaign auto-paused because its linked Package's price changed. */
public record AdCampaignPausedEvent(UUID campaignId, UUID consultantId) {
}
