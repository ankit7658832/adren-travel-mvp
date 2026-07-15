package com.adren.travel.whitelabel.internal;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FND-08 — the set of every currently-mapped Consultant CNAME domain,
 * backing {@code DynamicCorsConfigurationSource}'s per-request lookup so
 * CORS evaluation isn't a database round-trip on every request (including
 * every preflight OPTIONS). Same short-TTL-plus-commit-time-invalidation
 * shape as {@link BrandingCache} (FND-07) — {@link BrandingCacheInvalidationListener}
 * calls {@link #invalidate()} too, so a newly mapped/changed domain is
 * enforceable on the very next request, not after the TTL expires.
 */
@Component
class RegisteredDomainsCache {

    static final Duration TTL = Duration.ofSeconds(30);

    private final BrandingProfileRepository brandingProfileRepository;
    private final Clock clock;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();

    RegisteredDomainsCache(BrandingProfileRepository brandingProfileRepository, Clock clock) {
        this.brandingProfileRepository = brandingProfileRepository;
        this.clock = clock;
    }

    boolean isRegistered(String domain) {
        return currentDomains().contains(domain);
    }

    void invalidate() {
        snapshot.set(null);
    }

    private Set<String> currentDomains() {
        Snapshot current = snapshot.get();
        if (current == null || Duration.between(current.loadedAt(), clock.instant()).compareTo(TTL) > 0) {
            Set<String> reloaded = Set.copyOf(brandingProfileRepository.findAllDomains());
            current = new Snapshot(reloaded, clock.instant());
            snapshot.set(current);
        }
        return current.domains();
    }

    private record Snapshot(Set<String> domains, Instant loadedAt) {
    }
}
