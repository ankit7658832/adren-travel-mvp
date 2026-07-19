package com.adren.travel.booking;

import com.adren.travel.shared.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * PRD §16, §22.8 T12, HRD-07 — one PNR search hit. Deliberately carries no
 * product-type field: {@code Booking} itself is product-type-agnostic (its
 * line items live on the linked Itinerary across five separate tables), so
 * the SAME lookup returns a hotel, flight, transfer, cruise, or activity
 * booking identically — there is nothing here to branch on.
 */
public record BookingSearchResultView(
    UUID bookingId, String pnrSearchableRef, String status, Money totalSellPrice, String paymentMethod, Instant createdAt) {
}
