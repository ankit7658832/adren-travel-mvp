package com.adren.travel.booking;

import java.time.Instant;

/**
 * A confirmed Booking's Voucher (PRD §20.11, BOK-15) — {@code
 * atolCertificateReference} is {@code null} outside a UK dynamic
 * flight+hotel combo (PRD §17.2), and always {@code null} in this mock
 * phase (see {@code VoucherService}'s own Javadoc for why).
 */
public record VoucherView(String pdfReference, String atolCertificateReference, Instant generatedAt) {
}
