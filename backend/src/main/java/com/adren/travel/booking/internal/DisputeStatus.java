package com.adren.travel.booking.internal;

/**
 * PRD §12.5, FIN-16 — "a dispute create a tracked ticket, not just an
 * email handoff." A single {@code OPEN} status is all this story's AC
 * requires (the ticket exists and is tracked); a full resolution
 * lifecycle (investigating/resolved/escalated) isn't specified anywhere
 * in the PRD and isn't built here — a documented, scoped omission.
 */
enum DisputeStatus {
    OPEN
}
