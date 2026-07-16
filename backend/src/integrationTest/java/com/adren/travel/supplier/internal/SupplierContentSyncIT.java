package com.adren.travel.supplier.internal;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BOK-27's core acceptance criterion, proven against a real (containerized)
 * Postgres: a content-sync run writes to the cache table, a second run for
 * the same (supplier, content) key refreshes the same row rather than
 * duplicating it, and staleness is detectable per the threshold HRD-13
 * alerts on.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
class SupplierContentSyncIT {

    @Autowired
    SupplierContentSyncService syncService;

    @Autowired
    SupplierContentCacheRepository repository;

    @Test
    void syncHotelContentWritesACacheRowPerHotelSupplier() {
        syncService.syncHotelContent();

        assertThat(repository.findBySupplierIdAndSupplierContentId(SupplierId.HOTELBEDS, "stub-rate-key")).isPresent();
        assertThat(repository.findBySupplierIdAndSupplierContentId(SupplierId.STUBA, "stub-offer-id")).isPresent();
        assertThat(repository.findBySupplierIdAndSupplierContentId(SupplierId.TBO, "stub-result-index")).isPresent();
    }

    @Test
    void repeatedSyncRunsRefreshTheSameRowRatherThanDuplicatingIt() {
        syncService.syncHotelContent();
        syncService.syncHotelContent();

        long matchingRows = repository.findAll().stream()
            .filter(c -> c.getSupplierId() == SupplierId.HOTELBEDS && c.getSupplierContentId().equals("stub-rate-key"))
            .count();
        assertThat(matchingRows).isEqualTo(1);
    }

    @Test
    void contentNeverSyncedIsReportedStale() {
        boolean stale = syncService.isStale(SupplierId.WIDGETY, "never-synced-sailing-id", Duration.ofHours(24));

        assertThat(stale).isTrue();
    }

    @Test
    void freshlySyncedContentIsNotReportedStale() {
        syncService.syncCruiseContent();

        boolean stale = syncService.isStale(SupplierId.WIDGETY, "stub-sailing-id", Duration.ofHours(24));

        assertThat(stale).isFalse();
    }
}
