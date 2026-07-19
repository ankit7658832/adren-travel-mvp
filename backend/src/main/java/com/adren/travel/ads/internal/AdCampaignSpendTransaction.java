package com.adren.travel.ads.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One spend increment recorded against a campaign (PRD §14.3's
 * billing-transparency requirement, ADS-11) — package-private, own table,
 * written by {@link AdCampaignSpendCapEnforcementService} once per poll
 * (ADS-10) so a Consultant's billing detail view shows an itemized
 * breakdown, not just {@code AdCampaign#getSpendToDateAmount}'s single
 * running total.
 */
@Entity
@Table(name = "ad_campaign_spend_transaction")
class AdCampaignSpendTransaction {

    @Id
    private UUID transactionId;

    private UUID campaignId;
    private BigDecimal amount;
    private Instant recordedAt;

    protected AdCampaignSpendTransaction() {
        // JPA
    }

    AdCampaignSpendTransaction(UUID transactionId, UUID campaignId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.campaignId = campaignId;
        this.amount = amount;
        this.recordedAt = Instant.now();
    }

    UUID getTransactionId() {
        return transactionId;
    }

    UUID getCampaignId() {
        return campaignId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    Instant getRecordedAt() {
        return recordedAt;
    }
}
