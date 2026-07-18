package com.adren.travel.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * Public API of the AI Itinerary &amp; Governance module (PRD §11). Other
 * modules must depend on this interface, never on classes under
 * {@code com.adren.travel.ai.internal}.
 */
public interface AiApi {

    /**
     * Generates a grounded itinerary suggestion (PRD §11.1/§11.2) —
     * invoked from {@code BookingApi.generateAiItinerarySuggestion} (the
     * "Complete with AI" entry point's backend, BOK's {@code Itinerary}
     * being the aggregate this suggestion is FOR). Same tenant-scoping
     * shape as every other tenant-scoped write — {@code consultantId} is
     * checked against the caller's own tenant via {@code
     * CurrentPrincipal.resolveTenantScope}. Always returns (never throws
     * for "no viable option" — see {@link AiItineraryGenerationResult}'s
     * Javadoc); only throws for genuine infrastructure failure (Groq
     * unreachable/misconfigured — PRD §11.2 principle 4's "explicit
     * failure state" is about business outcomes, not about masking a real
     * system fault as if it were a normal empty result).
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    AiItineraryGenerationResult generateItinerary(GenerateItineraryCommand command);

    /**
     * Records a Consultant's approval of one AI suggestion, capturing
     * BOTH the original AI output (already permanently in the referenced
     * {@code AiSuggestionAuditLog} row, untouched) and whatever the
     * Consultant is actually approving — edited or not (PRD §23.3 Edge
     * Case #8, §25 T14, AI-08). Invoked from {@code
     * BookingApi.approveAiSuggestion}, alongside that call's own {@code
     * Itinerary.markAiApproved()} — this method only writes the audit
     * trail, it does not itself gate {@code markAsQuotation} (AI-06
     * already does that on the {@code booking}-owned entity). Same
     * tenant-scoping shape as {@link #generateItinerary} — the audit log
     * row's own {@code consultantId} is checked against the caller's
     * tenant.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    void approveAiSuggestion(ApproveAiSuggestionCommand command);

    /**
     * Re-validates an approved AI suggestion's line items against LIVE
     * supplier data (PRD §11.3, AI-09) — invoked internally by {@code
     * BookingApi.confirmBooking}/{@code confirmBookingOnAccount} for
     * itineraries where {@code Itinerary.isAiGenerated()}, immediately
     * before the booking is finalized, following the same "price changed,
     * please confirm" pattern PRD §10.2.4 already requires for Mystifly
     * fare expiry. Re-runs {@code SupplierSearchApi.searchHotels} against
     * the ORIGINAL search parameters captured in the audit log's {@code
     * requestInputJson} and compares every approved {@code
     * supplierRateId}'s live net rate against what was actually approved
     * (the Consultant's edited final version if one exists, else the
     * original suggestion) — never re-trusts a cached/remembered price.
     * Same tenant-scoping shape as {@link #generateItinerary}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    AiPricingRevalidationResult revalidateAiPricingAtBooking(UUID auditLogId);

    /**
     * Generates grounded ad-creative variants for a Package (PRD §14.4,
     * AI-12) — invoked from {@code ads.AdsApi.generateAdCreativeForPackage},
     * which resolves the REAL, live Package content via {@code
     * BookingApi.findPackageById} and passes it in as {@link
     * GenerateAdCreativeCommand}'s grounding fields (this method never
     * reaches into {@code booking}'s data itself — same "caller supplies
     * verified grounding input" shape {@link #generateItinerary} already
     * uses). Every variant's {@code bodyText} is checked (server-side, not
     * just prompted) to literally contain the package's real name and
     * exact current sell price before being returned — a variant that
     * fails this check is dropped, never surfaced. 100%-audit-logged, same
     * transactional-gate shape as {@link #generateItinerary} (AI-07,
     * backend-best-practices §7). Same tenant-scoping shape as {@link
     * #generateItinerary}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    AdCreativeGenerationResult generateAdCreative(GenerateAdCreativeCommand command);

    /**
     * Browses the full AI suggestion audit trail (PRD §6 — "View AI
     * governance/audit logs (Yes, all)", §21.6, AI-11), every input/
     * source-data/output/disposition, optionally filtered to one
     * Consultant. {@code SUPER_ADMIN}-only — unlike every other method on
     * this Api, a Consultant/User has no equivalent "my own" view here;
     * PRD §6 grants this company-wide visibility to Super Admin alone, not
     * a self-scoped subset the way most of this Api's methods work.
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    Page<AiAuditLogEntryView> findAuditLog(UUID consultantId, Pageable pageable);
}
