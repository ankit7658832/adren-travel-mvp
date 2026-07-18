package com.adren.travel.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

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

    /**
     * Submits a new Local DMC for onboarding (PRD §10.3 step 1, DMC-01) —
     * always {@code PENDING}, never immediately visible/sellable, per the
     * load-bearing invariant {@code LocalDmcRecord}'s constructor enforces.
     * {@code Yes/Yes/No} across Super Admin/Consultant/User per the story's
     * own "As a Consultant" framing — narrower than the search/booking
     * methods above, which are Yes/Yes/Yes.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    UUID submitLocalDmc(SubmitLocalDmcCommand command);

    /**
     * Reviews a Pending Local DMC and, only once at least one verification
     * step is recorded, transitions it to Active (PRD §10.3 steps 2-3,
     * DMC-02) — throws {@link LocalDmcVerificationRequiredException} (409)
     * rather than silently allowing the transition otherwise.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    void activateLocalDmc(UUID localDmcId, ActivateLocalDmcCommand command);

    /**
     * Browses Local DMC records — a Consultant always sees only their own
     * (regardless of {@code consultantId}, which this scaffold has no way
     * to derive client-side yet, no login/session story having landed);
     * Super Admin sees every Consultant's when {@code consultantId} is
     * {@code null}, or one Consultant's when supplied — same shape as
     * {@code AiApi.findAuditLog}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    Page<LocalDmcView> findLocalDmcs(UUID consultantId, Pageable pageable);
}
