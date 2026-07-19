package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-13's own acceptance criterion against real Postgres: a supplier's
 * cached content past its staleness threshold gets flagged, proving the
 * new {@code stale} column (V40) round-trips through real Hibernate
 * mapping — the kind of bug a mocked-repository unit test can't catch.
 * Lives in {@code supplier.internal} (not {@code supplier}) to reach the
 * package-private {@link SupplierContentStalenessCheckService} directly,
 * same reasoning as {@code NotificationRegionRoutingIntegrationTest}'s own
 * package placement.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "security")
class SupplierContentStalenessCheckIntegrationTest {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    SupplierContentStalenessCheckService checkService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void hotelContentPastTheFortyEightHourThresholdIsFlaggedStaleAndPersistedHRD13() {
        UUID id = insertCacheRow(SupplierId.HOTELBEDS, Instant.now().minus(50, ChronoUnit.HOURS));

        checkService.checkStaleness();

        Boolean stale = jdbcTemplate.queryForObject("SELECT stale FROM supplier_content_cache WHERE id = ?", Boolean.class, id);
        assertThat(stale).isTrue();
    }

    @Test
    void freshHotelContentIsNotFlaggedStaleHRD13() {
        UUID id = insertCacheRow(SupplierId.STUBA, Instant.now().minus(1, ChronoUnit.HOURS));

        checkService.checkStaleness();

        Boolean stale = jdbcTemplate.queryForObject("SELECT stale FROM supplier_content_cache WHERE id = ?", Boolean.class, id);
        assertThat(stale).isFalse();
    }

    private UUID insertCacheRow(SupplierId supplierId, Instant lastSyncedAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO supplier_content_cache (id, supplier_id, supplier_content_id, name, rating, last_synced_at, stale) " +
                "VALUES (?, ?, ?, 'Test Content', 4.0, ?, false)",
            id, supplierId.name(), "hrd13-test-" + id, java.sql.Timestamp.from(lastSyncedAt));
        return id;
    }
}
