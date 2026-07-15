package com.adren.travel.booking.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A booking confirmation Voucher (PRD §20.11, BOK-15) — package-private,
 * own table. {@code bookingId} is a plain UUID, not an FK: {@code
 * confirmBooking} doesn't yet persist a real Booking entity (still a
 * "simplified" stub per its own long-standing comment), so there's no
 * {@code booking} table to reference. {@code atolCertificateReference} is
 * populated only for a UK dynamic flight+hotel package (PRD §17.2, §22.9
 * T5) — always {@code null} in this vertical slice, since no Flight line
 * item type exists yet (BOK-04) and {@code TravelPackage.dynamicFlightHotelCombo}
 * is hard-defaulted {@code false} until it does (see BOK-11's deferral note).
 */
@Entity
@Table(name = "voucher")
class Voucher {

    @Id
    private UUID voucherId;

    private UUID bookingId;
    private String pdfReference;
    private Instant generatedAt;
    private String atolCertificateReference;

    protected Voucher() {
        // JPA
    }

    Voucher(UUID voucherId, UUID bookingId, String pdfReference, String atolCertificateReference) {
        this.voucherId = voucherId;
        this.bookingId = bookingId;
        this.pdfReference = pdfReference;
        this.generatedAt = Instant.now();
        this.atolCertificateReference = atolCertificateReference;
    }

    UUID getVoucherId() {
        return voucherId;
    }

    UUID getBookingId() {
        return bookingId;
    }

    String getPdfReference() {
        return pdfReference;
    }

    Instant getGeneratedAt() {
        return generatedAt;
    }

    String getAtolCertificateReference() {
        return atolCertificateReference;
    }
}
