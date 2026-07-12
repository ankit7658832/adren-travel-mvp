/**
 * Cross-cutting Security module (RULES.md Section 5; PRD Section 6 Roles &amp;
 * Permissions Matrix).
 * <p>
 * Owns authentication (stateless JWT) and the {@link com.adren.travel.security.AdrenPrincipal}
 * every other module reads to enforce authorization. Public surface is
 * {@link com.adren.travel.security.AdrenPrincipal}, {@link com.adren.travel.security.Role},
 * {@link com.adren.travel.security.CurrentPrincipal}, and
 * {@link com.adren.travel.security.CapabilityGrantService} — everything under
 * {@code .internal} (JWT signing/parsing, the filter chain, the security
 * filter configuration) is invisible to other modules, enforced by
 * {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Security"
)
package com.adren.travel.security;
