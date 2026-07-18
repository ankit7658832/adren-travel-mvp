package com.adren.travel.supplier.internal.localdmc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocalDmcRecordRepository extends JpaRepository<LocalDmcRecord, UUID> {

    Page<LocalDmcRecord> findByConsultantId(UUID consultantId, Pageable pageable);

    /** DMC-11 — every ACTIVE record, checked by the scheduled staleness job. */
    List<LocalDmcRecord> findByStatus(LocalDmcStatus status);
}
