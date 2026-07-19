package com.adren.travel.booking.event;

import java.util.UUID;

/** ADS-12, PRD §23.5 Edge Case #11 — a Package's price changed; {@code ads} reacts by auto-pausing any Live campaign promoting it. */
public record PackagePriceChangedEvent(UUID packageId, UUID consultantId) {
}
