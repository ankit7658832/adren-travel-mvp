package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierContentStalenessCheckServiceTest {

    @Mock
    SupplierContentCacheRepository repository;

    SupplierContentStalenessCheckService service;

    @BeforeEach
    void setUp() {
        // Matches application.yml's defaults: 48h for hotels/activities, 14d for cruise.
        SupplierContentStalenessThresholds thresholds =
            new SupplierContentStalenessThresholds(Duration.ofHours(48), Duration.ofDays(14), Duration.ofHours(48));
        service = new SupplierContentStalenessCheckService(repository, thresholds);
    }

    @Test
    void hotelContentSyncedJustInsideTheFortyEightHourThresholdIsNotStaleHRD13() {
        SupplierContentCache cache = cacheSyncedAt(SupplierId.HOTELBEDS, Instant.now().minus(Duration.ofHours(47)));
        when(repository.findAll()).thenReturn(List.of(cache));

        service.checkStaleness();

        assertThat(cache.isStale()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void hotelContentSyncedJustOutsideTheFortyEightHourThresholdIsStaleHRD13() {
        SupplierContentCache cache = cacheSyncedAt(SupplierId.HOTELBEDS, Instant.now().minus(Duration.ofHours(49)));
        when(repository.findAll()).thenReturn(List.of(cache));

        service.checkStaleness();

        assertThat(cache.isStale()).isTrue();
        verify(repository).save(cache);
    }

    @Test
    void cruiseContentUsesTheFourteenDayThresholdNotTheFortyEightHourOneHRD13() {
        // Would be stale under the hotel/activity threshold, but cruise syncs weekly.
        SupplierContentCache cache = cacheSyncedAt(SupplierId.WIDGETY, Instant.now().minus(Duration.ofDays(10)));
        when(repository.findAll()).thenReturn(List.of(cache));

        service.checkStaleness();

        assertThat(cache.isStale()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void aPreviouslyStaleRowIsClearedOnceItsContentIsFreshAgainHRD13() {
        SupplierContentCache cache = cacheSyncedAt(SupplierId.HBACTIVITIES, Instant.now());
        cache.setStale(true);
        when(repository.findAll()).thenReturn(List.of(cache));

        service.checkStaleness();

        assertThat(cache.isStale()).isFalse();
        verify(repository).save(cache);
    }

    @Test
    void aNeverContentSyncedSupplierRowThrowsSinceThisMethodOnlyEverSeesRowsItsOwnModuleWroteHRD13() {
        SupplierContentCache cache = cacheSyncedAt(SupplierId.MYSTIFLY, Instant.now());
        when(repository.findAll()).thenReturn(List.of(cache));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, service::checkStaleness);
    }

    private static SupplierContentCache cacheSyncedAt(SupplierId supplierId, Instant syncedAt) {
        SupplierContentCache cache = new SupplierContentCache(supplierId, "content-id");
        cache.refresh("Name", 4.0);
        setLastSyncedAt(cache, syncedAt);
        return cache;
    }

    private static void setLastSyncedAt(SupplierContentCache cache, Instant instant) {
        try {
            var field = SupplierContentCache.class.getDeclaredField("lastSyncedAt");
            field.setAccessible(true);
            field.set(cache, instant);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
