package com.adren.travel.booking.internal;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Generates and persists the Voucher for a just-confirmed booking (PRD
 * §20.11, BOK-15) — PDF rendering itself is stubbed (a placeholder byte
 * payload) pending a real PDF library, which is out of this vertical
 * slice's scope; the entity/storage-seam/ATOL-conditional wiring is real.
 * {@code atolCertificateReference} is always {@code null} here — see
 * {@link Voucher}'s Javadoc for why (no Flight line item type exists yet
 * to ever produce a UK dynamic flight+hotel combo).
 */
@Component
class VoucherService {

    private final VoucherRepository voucherRepository;
    private final DocumentStorage documentStorage;

    VoucherService(VoucherRepository voucherRepository, DocumentStorage documentStorage) {
        this.voucherRepository = voucherRepository;
        this.documentStorage = documentStorage;
    }

    Voucher generateFor(UUID bookingId) {
        UUID voucherId = UUID.randomUUID();
        byte[] pdfContent = ("Voucher for booking " + bookingId).getBytes(StandardCharsets.UTF_8);
        String pdfReference = documentStorage.store("vouchers/" + bookingId, pdfContent);

        Voucher voucher = new Voucher(voucherId, bookingId, pdfReference, null);
        voucherRepository.save(voucher);
        return voucher;
    }

    /** SCR-17 — the Download Voucher button's actual content. */
    byte[] retrievePdf(UUID bookingId) {
        Voucher voucher = voucherRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new IllegalStateException("No voucher for confirmed booking: " + bookingId));
        return documentStorage.retrieve(voucher.getPdfReference());
    }
}
