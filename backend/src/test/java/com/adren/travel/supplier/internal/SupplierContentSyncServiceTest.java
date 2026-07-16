package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierContentSyncServiceTest {

    @Mock
    SupplierContentCacheRepository repository;

    SupplierContentSyncService service;

    @BeforeEach
    void setUp() {
        service = new SupplierContentSyncService(repository);
    }

    @Test
    void syncHotelContentSyncsHotelbedsStubaAndTboOnTheSameNightlyRun() {
        when(repository.findBySupplierIdAndSupplierContentId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(Optional.empty());

        service.syncHotelContent();

        ArgumentCaptor<SupplierContentCache> captor = ArgumentCaptor.forClass(SupplierContentCache.class);
        verify(repository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(SupplierContentCache::getSupplierId)
            .containsExactlyInAnyOrder(SupplierId.HOTELBEDS, SupplierId.STUBA, SupplierId.TBO);
    }

    @Test
    void syncCruiseContentSyncsWidgetyOnTheWeeklyRun() {
        when(repository.findBySupplierIdAndSupplierContentId(SupplierId.WIDGETY, "stub-sailing-id"))
            .thenReturn(Optional.empty());

        service.syncCruiseContent();

        ArgumentCaptor<SupplierContentCache> captor = ArgumentCaptor.forClass(SupplierContentCache.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSupplierId()).isEqualTo(SupplierId.WIDGETY);
    }

    @Test
    void isStaleReturnsTrueWhenNoSyncHasEverRunForThatContent() {
        when(repository.findBySupplierIdAndSupplierContentId(SupplierId.HBACTIVITIES, "never-synced"))
            .thenReturn(Optional.empty());

        assertThat(service.isStale(SupplierId.HBACTIVITIES, "never-synced", Duration.ofHours(24))).isTrue();
    }

    @Test
    void isStaleReturnsTrueWhenTheLastSyncIsOlderThanTheThreshold() {
        SupplierContentCache stale = new SupplierContentCache(SupplierId.HBACTIVITIES, "old-activity");
        stale.refresh("Old Activity", 4.0);
        setLastSyncedAt(stale, Instant.now().minus(Duration.ofDays(2)));
        when(repository.findBySupplierIdAndSupplierContentId(SupplierId.HBACTIVITIES, "old-activity"))
            .thenReturn(Optional.of(stale));

        assertThat(service.isStale(SupplierId.HBACTIVITIES, "old-activity", Duration.ofHours(24))).isTrue();
    }

    @Test
    void isStaleReturnsFalseWhenTheLastSyncIsWithinTheThreshold() {
        SupplierContentCache fresh = new SupplierContentCache(SupplierId.HBACTIVITIES, "fresh-activity");
        fresh.refresh("Fresh Activity", 4.0);
        when(repository.findBySupplierIdAndSupplierContentId(SupplierId.HBACTIVITIES, "fresh-activity"))
            .thenReturn(Optional.of(fresh));

        assertThat(service.isStale(SupplierId.HBACTIVITIES, "fresh-activity", Duration.ofHours(24))).isFalse();
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
