package com.adren.travel.ads.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One Adren-managed Meta ad account/Business Manager per Consultant
 * (PRD §14.1, ADS-01) — never Consultant-owned, per PRD §6's "No
 * (executes)" row. Package-private; other modules never see this entity,
 * only {@code AdsApi}.
 */
@Entity
@Table(name = "ad_account")
class AdAccount {

    @Id
    private UUID adAccountId;

    private UUID consultantId;

    @Column(name = "meta_business_manager_id")
    private String metaBusinessManagerId;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    protected AdAccount() {
        // JPA
    }

    AdAccount(UUID adAccountId, UUID consultantId, String metaBusinessManagerId) {
        this.adAccountId = adAccountId;
        this.consultantId = consultantId;
        this.metaBusinessManagerId = metaBusinessManagerId;
        this.provisionedAt = Instant.now();
    }

    UUID getAdAccountId() {
        return adAccountId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getMetaBusinessManagerId() {
        return metaBusinessManagerId;
    }

    Instant getProvisionedAt() {
        return provisionedAt;
    }
}
