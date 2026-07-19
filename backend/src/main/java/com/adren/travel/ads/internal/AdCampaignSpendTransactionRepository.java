package com.adren.travel.ads.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AdCampaignSpendTransactionRepository extends JpaRepository<AdCampaignSpendTransaction, UUID> {

    /** ADS-11 — the billing-detail view's per-transaction breakdown, newest first. */
    List<AdCampaignSpendTransaction> findByCampaignIdOrderByRecordedAtDesc(UUID campaignId);
}
