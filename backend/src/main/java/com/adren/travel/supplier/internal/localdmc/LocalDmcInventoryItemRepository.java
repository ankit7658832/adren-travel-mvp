package com.adren.travel.supplier.internal.localdmc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalDmcInventoryItemRepository extends JpaRepository<LocalDmcInventoryItem, UUID> {

    Page<LocalDmcInventoryItem> findByLocalDmcId(UUID localDmcId, Pageable pageable);

    List<LocalDmcInventoryItem> findByLocalDmcId(UUID localDmcId);

    /** DMC-11 — the newest item's {@code updatedAt} IS the DMC's own inventory freshness. */
    Optional<LocalDmcInventoryItem> findTopByLocalDmcIdOrderByUpdatedAtDesc(UUID localDmcId);
}
