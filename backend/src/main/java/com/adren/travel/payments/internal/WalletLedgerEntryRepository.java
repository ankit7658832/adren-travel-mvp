package com.adren.travel.payments.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, UUID> {

    boolean existsByRelatedBookingIdAndType(UUID relatedBookingId, LedgerEntryType type);

    List<WalletLedgerEntry> findByConsultantId(UUID consultantId);
}
