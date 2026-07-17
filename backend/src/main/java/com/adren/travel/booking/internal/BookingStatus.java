package com.adren.travel.booking.internal;

/** Per PRD Section 20.8 — booking.status field. Only CONFIRMED is reachable today (no cancel/dispute flow wired in yet). */
enum BookingStatus {
    CONFIRMED,
    PARTIALLY_CANCELLED,
    CANCELLED,
    DISPUTED
}
