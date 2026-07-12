package com.adren.travel.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Reads the {@link AdrenPrincipal} the JWT filter chain attached to the
 * current request thread. This is the one place a service method should go
 * to find "who is calling me" — never trust a client-supplied consultantId
 * for tenant scoping (RULES.md §5.2).
 */
public final class CurrentPrincipal {

    private CurrentPrincipal() {
    }

    public static AdrenPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdrenPrincipal principal)) {
            throw new AccessDeniedException("No authenticated ADREN principal on this request");
        }
        return principal;
    }

    /**
     * Resolves the consultantId a tenant-scoped query should actually run
     * with: the caller's own consultantId, unless the caller is SUPER_ADMIN
     * exercising the explicit "view all" path, in which case the
     * caller-supplied value passes through unchanged. Rejects (403) any
     * attempt by a CONSULTANT/USER to query another tenant's data.
     */
    public static UUID resolveTenantScope(UUID requestedConsultantId) {
        AdrenPrincipal principal = get();
        if (principal.isSuperAdmin()) {
            return requestedConsultantId;
        }
        if (!principal.consultantId().equals(requestedConsultantId)) {
            throw new AccessDeniedException(
                "Principal is not authorized to access consultant " + requestedConsultantId);
        }
        return principal.consultantId();
    }
}
