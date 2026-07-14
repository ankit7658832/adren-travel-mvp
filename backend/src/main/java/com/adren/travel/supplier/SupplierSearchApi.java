package com.adren.travel.supplier;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Public API of the Supplier module. Normalizes results across all 9 sources
 * (PRD Section 10.1) into {@link SupplierSearchResult} so the Booking module
 * never needs to know which supplier a given result came from until it's
 * displayed (PRD Section 9.4 — duplicate/normalization handling).
 * <p>
 * PRD §6's "Search &amp; build itinerary" row is Yes/Yes/Yes across Super
 * Admin/Consultant/User — see {@link com.adren.travel.booking.BookingApi}'s
 * class Javadoc for why that's still an explicit {@code @PreAuthorize}
 * rather than left unannotated.
 */
public interface SupplierSearchApi {

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    List<SupplierSearchResult> searchHotels(String locationCode, java.time.LocalDate checkIn, java.time.LocalDate checkOut);

    /**
     * Adds/rotates an Adren-owned supplier credential (PRD §21.6, §10.2) —
     * Super Admin only per PRD §6. Never logs or returns the raw
     * {@code secretValue} (RULES.md §5.3/§6.2).
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    void updateSupplierCredential(UpdateSupplierCredentialCommand command);

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    List<SupplierCredentialSummary> listSupplierCredentials();
}
