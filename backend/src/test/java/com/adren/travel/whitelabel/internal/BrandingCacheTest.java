package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.BrandingProfileView;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** FND-07 / PRD §24.5 — the short-TTL branding read cache. */
class BrandingCacheTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final BrandingCache cache = new BrandingCache(clock);

    @Test
    void aFreshEntryIsReturnedWithinTheTtlWindow() {
        UUID consultantId = UUID.randomUUID();
        BrandingProfileView profile = sampleProfile(consultantId);
        cache.put(consultantId, profile);

        clock.advance(BrandingCache.TTL.minusSeconds(1));

        assertThat(cache.get(consultantId)).contains(profile);
    }

    @Test
    void anEntryOlderThanTheTtlIsTreatedAsAMiss() {
        UUID consultantId = UUID.randomUUID();
        cache.put(consultantId, sampleProfile(consultantId));

        clock.advance(BrandingCache.TTL.plusSeconds(1));

        assertThat(cache.get(consultantId)).isEmpty();
    }

    @Test
    void anUnknownConsultantIdIsAMiss() {
        assertThat(cache.get(UUID.randomUUID())).isEmpty();
    }

    @Test
    void evictRemovesTheEntryEvenWithinTheTtlWindowFND07() {
        UUID consultantId = UUID.randomUUID();
        cache.put(consultantId, sampleProfile(consultantId));

        cache.evict(consultantId);

        assertThat(cache.get(consultantId)).isEmpty();
    }

    private static BrandingProfileView sampleProfile(UUID consultantId) {
        return new BrandingProfileView(consultantId, "https://cdn/logo.png", null,
            "#FFFFFF", "#000000", "#111111", "consultant.example.com", Instant.now());
    }

    /** A settable {@link Clock} test double — advances only when told to, unlike {@link Clock#fixed}. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
