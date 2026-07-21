package com.adren.travel.booking;

import java.util.UUID;

/** HRD-09, PRD §9.5/§21.5 — the Consultant Dashboard's Top Packages tab, ranked by booking count. */
public record PackageSummaryView(UUID packageId, String name, long bookingCount) {
}
