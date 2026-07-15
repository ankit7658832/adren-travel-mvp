package com.adren.travel.whitelabel.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URI;
import java.util.List;

/**
 * RULES.md §5.4 / FND-08 — CORS resolved per-request from the whitelabel
 * domain registry, never a wildcard. Lives in {@code whitelabel.internal}
 * (not {@code security}, which every other module — including
 * {@code whitelabel} itself — depends ON) so wiring this into
 * {@code SecurityConfig} doesn't create a {@code security -> whitelabel}
 * edge back onto a module that already depends on {@code security}: the
 * only thing {@code security.internal.SecurityConfig} injects is the
 * framework-owned {@link CorsConfigurationSource} interface, not this
 * concrete class — Spring wires the implementation in from wherever it
 * lives, and Spring Modulith's dependency graph only tracks references
 * between this project's own {@code com.adren.travel.*} packages.
 * <p>
 * An unmapped/unknown origin gets a {@code null} return, per
 * {@link CorsConfigurationSource}'s contract — no CORS headers are added
 * to the response at all, which is what makes the browser block the
 * cross-origin request; there is deliberately no fallback branch that
 * returns an allow-all configuration.
 */
@Component
class DynamicCorsConfigurationSource implements CorsConfigurationSource {

    private final RegisteredDomainsCache registeredDomainsCache;

    DynamicCorsConfigurationSource(RegisteredDomainsCache registeredDomainsCache) {
        this.registeredDomainsCache = registeredDomainsCache;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || !registeredDomainsCache.isRegistered(hostOf(origin))) {
            return null;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        // The exact, single origin that matched — never a wildcard or the
        // raw Origin header echoed back without having been checked first.
        configuration.setAllowedOrigins(List.of(origin));
        configuration.setAllowedMethods(List.of(
            HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        return configuration;
    }

    private static String hostOf(String origin) {
        try {
            return URI.create(origin).getHost();
        } catch (IllegalArgumentException malformed) {
            return "";
        }
    }
}
