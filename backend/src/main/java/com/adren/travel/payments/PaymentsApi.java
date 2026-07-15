package com.adren.travel.payments;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the Payments, Yield/Markup &amp; Wallet module. Other
 * modules must depend on this interface, never on classes under
 * {@code com.adren.travel.payments.internal}.
 */
public interface PaymentsApi {

    /**
     * Configures a Consultant's markup rule for one product category (PRD
     * §12.1). PRD §3.3 — a Consultant's own Users "cannot change markup...
     * unless granted", so this is {@code CONSULTANT}-only (self-scoped to
     * the caller's own account) rather than the {@code CONSULTANT,USER}
     * shape most of {@code BookingApi} uses; {@code SUPER_ADMIN} retains
     * the usual oversight path. {@code consultantId} is checked against
     * the caller's own tenant via {@code CurrentPrincipal.resolveTenantScope}
     * (RULES.md §5.2), the same as every other tenant-scoped lookup.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    void configureMarkup(UUID consultantId, ConfigureMarkupCommand command);

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    List<MarkupRuleView> findMarkupRules(UUID consultantId);
}
