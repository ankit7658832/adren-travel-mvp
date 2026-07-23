package com.adren.travel.booking;

import com.adren.travel.shared.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * SCR-17 (doc/ADREN_UIUX_SPEC.md §12.2) — a confirmed Booking's
 * confirmation-screen content, never the JPA entity itself. {@code
 * status}/{@code paymentMethod} are plain strings (not the internal
 * {@code BookingStatus}/{@code PaymentMethod} enums, which are private
 * JPA-mapping details of this module) — the same "read-model, not the
 * entity" boundary {@code PackageView} already draws around {@code
 * PackageStatus}.
 */
public record BookingView(
    UUID bookingId,
    String pnrSearchableRef,
    String status,
    String paymentMethod,
    Money totalSellPrice,
    Instant createdAt,
    VoucherView voucher
) {
}
