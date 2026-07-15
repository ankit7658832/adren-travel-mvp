package com.adren.travel.whitelabel;

import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.shared.LocaleCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the White-Label &amp; Admin Console module. Other modules
 * must depend on this interface, never on classes under
 * {@code com.adren.travel.whitelabel.internal}.
 */
public interface WhitelabelApi {

    /**
     * Onboards a new Consultant (PRD §13.1) — Super Admin only per PRD §6's
     * role matrix ("Onboard Consultants": Yes/No/No).
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    UUID onboardConsultant(OnboardConsultantCommand command);

    /**
     * The data-driven KYC field set for a market (RULES.md §24.7 — never a
     * hardcoded per-market conditional) — public so the onboarding wizard
     * can render before a Consultant record exists yet.
     */
    List<KycFieldDefinition> requiredKycFieldsFor(Market market);

    /**
     * Adds a User under the CALLING Consultant's own account (PRD §3.3,
     * §6 "Add/manage Users under own account": Consultant only).
     * {@code command} never carries a consultantId — see its Javadoc.
     */
    @PreAuthorize("hasRole('CONSULTANT')")
    UUID addUser(AddUserCommand command);

    /**
     * Grants or revokes a capability for a User under the calling
     * Consultant's own account — rejected if the target User belongs to a
     * different Consultant (RULES.md §5.2).
     */
    @PreAuthorize("hasRole('CONSULTANT')")
    void setUserCapability(UUID userId, Capability capability, boolean granted);

    @PreAuthorize("hasRole('CONSULTANT')")
    Page<ConsultantUserView> findUsersByConsultant(Pageable pageable);

    /** Super Admin Console's Consultants list (PRD §21.6, FND-05), paginated per RULES.md §3.4. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    Page<ConsultantView> listConsultants(Pageable pageable);

    /** Pauses a Consultant's access (PRD §3.1) — its Users can no longer search/book until reinstated. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    void suspendConsultant(UUID consultantId);

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    void reinstateConsultant(UUID consultantId);

    /**
     * The tenant-status gate `booking`'s search/booking entry points call
     * before letting a request proceed (FND-05) — not role-restricted
     * itself, since it's consulted mid-flow by an already-authenticated
     * CONSULTANT/USER, not invoked directly as its own endpoint. Throws
     * {@link org.springframework.security.access.AccessDeniedException} if
     * the Consultant is SUSPENDED.
     */
    void requireConsultantActive(UUID consultantId);

    /**
     * The Consultant's home market (PRD §13.1) — same "consulted mid-flow
     * by an already-authorized system step" shape as {@link
     * #requireConsultantActive}, no {@code @PreAuthorize}. HRD-01's
     * region-routed secondary notification channel (WhatsApp for India/
     * Dubai, SMS elsewhere) is this method's first caller.
     */
    Market findConsultantMarket(UUID consultantId);

    /** Configures a Consultant's storefront branding (PRD §13.2, FND-06) — Super Admin only. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    void updateBranding(UpdateBrandingCommand command);

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    BrandingProfileView findBranding(UUID consultantId);

    /**
     * The data-driven per-market language catalog (PRD §13.3, RULES.md
     * §24.7 — never a hardcoded per-market conditional) — public, like
     * {@link #requiredKycFieldsFor(Market)}, so language settings can
     * render options regardless of caller role.
     */
    List<LocaleCode> availableLocalesFor(Market market);

    /**
     * Sets the CALLING Consultant's own preferred display language (PRD
     * §13.3) — rejected if {@code locale} isn't offered for the
     * Consultant's homeMarket per {@link #availableLocalesFor(Market)}.
     */
    @PreAuthorize("hasRole('CONSULTANT')")
    void changePreferredLocale(LocaleCode locale);
}
