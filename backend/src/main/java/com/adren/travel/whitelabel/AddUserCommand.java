package com.adren.travel.whitelabel;

/**
 * Adds a User under the CALLING Consultant's own account — {@code consultantId}
 * is deliberately not a field here; the service resolves it from
 * {@code CurrentPrincipal}, never a client-supplied value (RULES.md §5.2).
 */
public record AddUserCommand(String email, String displayName) {
}
