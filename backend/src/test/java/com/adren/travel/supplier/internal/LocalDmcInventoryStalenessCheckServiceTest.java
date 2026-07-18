package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItem;
import com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItemRepository;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecord;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecordRepository;
import com.adren.travel.supplier.internal.localdmc.LocalDmcStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDmcInventoryStalenessCheckServiceTest {

    @Mock
    LocalDmcRecordRepository recordRepository;

    @Mock
    LocalDmcInventoryItemRepository inventoryRepository;

    LocalDmcInventoryStalenessCheckService service;

    @BeforeEach
    void setUp() {
        // 30-day threshold, matching application.yml's default.
        LocalDmcQualityThresholds thresholds = new LocalDmcQualityThresholds(new BigDecimal("0.2"), 3, 30);
        service = new LocalDmcInventoryStalenessCheckService(recordRepository, inventoryRepository, thresholds);
    }

    @Test
    void aLocalDmcWithNoInventoryItemsAtAllIsStaleDMC11() {
        UUID localDmcId = UUID.randomUUID();
        when(inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)).thenReturn(Optional.empty());

        assertThat(service.isStale(localDmcId)).isTrue();
    }

    @Test
    void inventoryUpdatedJustInsideTheThresholdIsNotStaleDMC11() {
        UUID localDmcId = UUID.randomUUID();
        LocalDmcInventoryItem item = itemUpdatedAt(localDmcId, Instant.now().minus(29, ChronoUnit.DAYS));
        when(inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)).thenReturn(Optional.of(item));

        assertThat(service.isStale(localDmcId)).isFalse();
    }

    @Test
    void inventoryUpdatedJustOutsideTheThresholdIsStaleDMC11() {
        UUID localDmcId = UUID.randomUUID();
        LocalDmcInventoryItem item = itemUpdatedAt(localDmcId, Instant.now().minus(31, ChronoUnit.DAYS));
        when(inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)).thenReturn(Optional.of(item));

        assertThat(service.isStale(localDmcId)).isTrue();
    }

    @Test
    void checkInventoryStalenessFlagsAStaleActiveDmcAndPersistsItDMC11() {
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, UUID.randomUUID(), "Goa Local Tours", "TRANSFER", "x", "y");
        record.activate("Checked.");
        when(recordRepository.findByStatus(LocalDmcStatus.ACTIVE)).thenReturn(List.of(record));
        when(inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)).thenReturn(Optional.empty());

        service.checkInventoryStaleness();

        assertThat(record.isInventoryStale()).isTrue();
        verify(recordRepository).save(record);
    }

    @Test
    void checkInventoryStalenessLeavesAFreshActiveDmcUntouchedDMC11() {
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, UUID.randomUUID(), "Goa Local Tours", "TRANSFER", "x", "y");
        record.activate("Checked.");
        LocalDmcInventoryItem item = itemUpdatedAt(localDmcId, Instant.now());
        when(recordRepository.findByStatus(LocalDmcStatus.ACTIVE)).thenReturn(List.of(record));
        when(inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)).thenReturn(Optional.of(item));

        service.checkInventoryStaleness();

        assertThat(record.isInventoryStale()).isFalse();
        verify(recordRepository, never()).save(any());
    }

    @Test
    void checkInventoryStalenessClearsAPreviouslyStaleDmcOnceRefreshedDMC11() {
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, UUID.randomUUID(), "Goa Local Tours", "TRANSFER", "x", "y");
        record.activate("Checked.");
        record.setInventoryStale(true);
        LocalDmcInventoryItem item = itemUpdatedAt(localDmcId, Instant.now());
        when(recordRepository.findByStatus(LocalDmcStatus.ACTIVE)).thenReturn(List.of(record));
        when(inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)).thenReturn(Optional.of(item));

        service.checkInventoryStaleness();

        assertThat(record.isInventoryStale()).isFalse();
        verify(recordRepository).save(record);
    }

    private static LocalDmcInventoryItem itemUpdatedAt(UUID localDmcId, Instant updatedAt) {
        LocalDmcInventoryItem item = new LocalDmcInventoryItem(UUID.randomUUID(), localDmcId, "City Tour",
            com.adren.travel.shared.ProductCategory.ACTIVITY, new BigDecimal("2000"), com.adren.travel.shared.CurrencyCode.INR,
            "Free cancellation", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31));
        item.update(item.getProductName(), item.getCategory(), item.getNetRate(), item.getNetRateCurrency(),
            item.getCancellationPolicyText(), item.getAvailableFrom(), item.getAvailableTo());
        setUpdatedAtViaReflection(item, updatedAt);
        return item;
    }

    /** {@code updatedAt} is only ever set by the constructor/update() to "now" — reflection is the only way to backdate it for this test. */
    private static void setUpdatedAtViaReflection(LocalDmcInventoryItem item, Instant updatedAt) {
        try {
            var field = LocalDmcInventoryItem.class.getDeclaredField("updatedAt");
            field.setAccessible(true);
            field.set(item, updatedAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
