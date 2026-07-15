package com.adren.travel.whitelabel.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FND-08 — the CORS domain-registry cache backing {@code DynamicCorsConfigurationSource}. */
@ExtendWith(MockitoExtension.class)
class RegisteredDomainsCacheTest {

    @Mock
    BrandingProfileRepository brandingProfileRepository;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void aMappedDomainIsRegistered() {
        when(brandingProfileRepository.findAllDomains()).thenReturn(List.of("consultant.example.com"));
        RegisteredDomainsCache cache = new RegisteredDomainsCache(brandingProfileRepository, clock);

        assertThat(cache.isRegistered("consultant.example.com")).isTrue();
    }

    @Test
    void anUnmappedDomainIsNotRegistered() {
        when(brandingProfileRepository.findAllDomains()).thenReturn(List.of("consultant.example.com"));
        RegisteredDomainsCache cache = new RegisteredDomainsCache(brandingProfileRepository, clock);

        assertThat(cache.isRegistered("evil.example.com")).isFalse();
    }

    @Test
    void doesNotReloadFromTheRepositoryWithinTheTtlWindow() {
        when(brandingProfileRepository.findAllDomains()).thenReturn(List.of("consultant.example.com"));
        RegisteredDomainsCache cache = new RegisteredDomainsCache(brandingProfileRepository, clock);

        cache.isRegistered("consultant.example.com");
        clock.advance(RegisteredDomainsCache.TTL.minusSeconds(1));
        cache.isRegistered("consultant.example.com");

        verify(brandingProfileRepository, times(1)).findAllDomains();
    }

    @Test
    void reloadsFromTheRepositoryAfterTheTtlExpires() {
        when(brandingProfileRepository.findAllDomains()).thenReturn(List.of("consultant.example.com"));
        RegisteredDomainsCache cache = new RegisteredDomainsCache(brandingProfileRepository, clock);

        cache.isRegistered("consultant.example.com");
        clock.advance(RegisteredDomainsCache.TTL.plusSeconds(1));
        cache.isRegistered("consultant.example.com");

        verify(brandingProfileRepository, times(2)).findAllDomains();
    }

    @Test
    void invalidateForcesAReloadEvenWithinTheTtlWindowFND08() {
        when(brandingProfileRepository.findAllDomains())
            .thenReturn(List.of("first.example.com"))
            .thenReturn(List.of("second.example.com"));
        RegisteredDomainsCache cache = new RegisteredDomainsCache(brandingProfileRepository, clock);
        assertThat(cache.isRegistered("first.example.com")).isTrue();

        cache.invalidate();

        assertThat(cache.isRegistered("first.example.com")).isFalse();
        assertThat(cache.isRegistered("second.example.com")).isTrue();
    }

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
