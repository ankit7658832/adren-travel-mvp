package com.adren.travel.supplier.internal;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-12's own acceptance criterion end to end: overriding
 * {@code adren.supplier.content-sync.cruise-cron} takes effect without any
 * change to {@link SupplierContentSyncService} — the real Spring scheduler,
 * not a manually invoked method call, is what fires the job here (contrast
 * {@code SupplierContentSyncIT}, which calls {@code syncCruiseContent()}
 * directly to prove the sync/cache logic itself, not the cadence wiring).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
@TestPropertySource(properties = "adren.supplier.content-sync.cruise-cron=*/1 * * * * *")
class SupplierContentSyncCadenceIT {

    @Autowired
    SupplierContentCacheRepository repository;

    @Test
    void aSupplierCadenceOverriddenToEverySecondFiresWithoutAnyCodeChange() {
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(
                repository.findBySupplierIdAndSupplierContentId(SupplierId.WIDGETY, "stub-sailing-id"))
                .isPresent());
    }
}
