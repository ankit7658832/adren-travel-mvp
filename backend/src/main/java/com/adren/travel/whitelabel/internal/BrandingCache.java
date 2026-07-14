package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.BrandingProfileView;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FND-07 / PRD §24.5 — the short-TTL, in-process read cache backing
 * {@code WhitelabelServiceImpl#findBranding}, so the storefront's branding
 * lookup isn't a database round-trip on every page load. {@link #TTL} is
 * a safety-net staleness bound only; the actual "no redeploy needed"
 * guarantee comes from {@link BrandingCacheInvalidationListener} evicting
 * the entry the moment a save commits, so a Consultant's own change is
 * visible on their very next read — well inside the TTL window, not after
 * waiting for it to expire.
 */
@Component
class BrandingCache {

    static final Duration TTL = Duration.ofSeconds(30);

    private final ConcurrentHashMap<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    BrandingCache(Clock clock) {
        this.clock = clock;
    }

    Optional<BrandingProfileView> get(UUID consultantId) {
        Entry entry = entries.get(consultantId);
        if (entry == null || Duration.between(entry.cachedAt(), clock.instant()).compareTo(TTL) > 0) {
            return Optional.empty();
        }
        return Optional.of(entry.profile());
    }

    void put(UUID consultantId, BrandingProfileView profile) {
        entries.put(consultantId, new Entry(profile, clock.instant()));
    }

    void evict(UUID consultantId) {
        entries.remove(consultantId);
    }

    private record Entry(BrandingProfileView profile, Instant cachedAt) {
    }
}
