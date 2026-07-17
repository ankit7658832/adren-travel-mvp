package com.adren.travel.payments.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, UUID> {

    boolean existsByRelatedBookingIdAndType(UUID relatedBookingId, LedgerEntryType type);

    // FIN-09, RULES.md §3.4 — paginated all the way to the query, never an
    // unbounded List fetched then paged in memory (a Consultant can
    // accumulate thousands of ledger entries over time).
    Page<WalletLedgerEntry> findByConsultantId(UUID consultantId, Pageable pageable);

    Page<WalletLedgerEntry> findByConsultantIdAndType(UUID consultantId, LedgerEntryType type, Pageable pageable);
}
