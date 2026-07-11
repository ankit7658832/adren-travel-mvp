/**
 * Core Booking Engine module (PRD Section 9).
 * <p>
 * Owns Itinerary, Line Item, Package, Booking, Quotation lifecycle
 * (PRD Section 20.1-20.9). Public surface is {@link com.adren.travel.booking.BookingApi}
 * plus the events in {@link com.adren.travel.booking.event}. Everything under
 * {@code .internal} is invisible to other modules — enforced by
 * {@code ModularityTests} (see src/test/.../ModularityTests.java).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Core Booking Engine"
)
package com.adren.travel.booking;
