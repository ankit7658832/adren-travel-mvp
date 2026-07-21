package com.adren.travel.booking;

import com.adren.travel.shared.Money;

/** HRD-09, PRD §9.5/§21.5 — the Consultant Dashboard's "bookings this month"/"GMV" summary cards. */
public record ConsultantBookingMetricsView(int bookingsThisMonth, Money gmvThisMonth) {
}
