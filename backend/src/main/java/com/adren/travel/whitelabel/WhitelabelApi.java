package com.adren.travel.whitelabel;

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
}
