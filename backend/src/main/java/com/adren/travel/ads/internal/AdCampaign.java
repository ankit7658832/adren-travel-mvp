package com.adren.travel.ads.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ad Campaign entity per PRD §20.13 — package-private, own table. Owns the
 * PendingApproval -> PendingPolicyReview -> Live -> Paused/Rejected/
 * SpendCapReached state machine as named, entity-owned transition methods
 * (backend-best-practices §1, mirroring {@code Itinerary.markAsQuotation()}'s
 * established pattern) rather than scattered service-layer conditionals.
 * <p>
 * ADS-02 builds the complete state machine here, since that is literally
 * this story's own User Story ("an AdCampaign entity enforcing the ...
 * state machine"). ADS-05/06/07 each "extend" one of these pre-built
 * transitions into a full orchestrated business operation (a
 * {@code @Transactional} service method + domain event + REST endpoint) —
 * their own sub-tasks say "[EXTEND] ... business logic / state-transition
 * method", not "[NEW] entity method", which is the textual signal this
 * split follows. {@code submitCampaignInputs} (ADS-03, field-setting, not a
 * status transition) and per-variant creative approval (ADS-04/05,
 * collection management on a separate child table) are genuinely new
 * methods added by their own stories, not pre-built here.
 */
@Entity
@Table(name = "ad_campaign")
class AdCampaign {

    @Id
    private UUID campaignId;

    private UUID packageId;
    private UUID consultantId;

    @Enumerated(EnumType.STRING)
    private AdCampaignStatus status;

    @Column(name = "audience_description")
    private String audienceDescription;

    @Column(name = "budget_cap_amount")
    private BigDecimal budgetCapAmount;

    @Column(name = "budget_cap_currency")
    @Enumerated(EnumType.STRING)
    private CurrencyCode budgetCapCurrency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "meta_campaign_ref")
    private String metaCampaignRef;

    @Column(name = "spend_to_date_amount")
    private BigDecimal spendToDateAmount;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private long version;

    protected AdCampaign() {
        // JPA
    }

    AdCampaign(UUID campaignId, UUID packageId, UUID consultantId, CurrencyCode budgetCapCurrency) {
        this.campaignId = campaignId;
        this.packageId = packageId;
        this.consultantId = consultantId;
        this.budgetCapCurrency = budgetCapCurrency;
        this.status = AdCampaignStatus.PENDING_APPROVAL;
        this.spendToDateAmount = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** ADS-05 — a Consultant submits the campaign for Super Admin review once every creative variant is approved (checked by the caller; see class Javadoc). */
    void submitForPolicyReview() {
        if (this.status != AdCampaignStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "Only a PENDING_APPROVAL campaign can be submitted for policy review, was: " + this.status);
        }
        this.status = AdCampaignStatus.PENDING_POLICY_REVIEW;
        this.updatedAt = Instant.now();
    }

    /** ADS-06 — Super Admin rejects a campaign under policy review; {@code reason} is surfaced to the Consultant. */
    void rejectPolicyReview(String reason) {
        if (this.status != AdCampaignStatus.PENDING_POLICY_REVIEW) {
            throw new IllegalStateException(
                "Only a PENDING_POLICY_REVIEW campaign can be rejected, was: " + this.status);
        }
        this.status = AdCampaignStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }

    /** ADS-07 — Super Admin's policy-review approval doubles as launch, since PRD §20.13's enum has no separate "Approved" state between PendingPolicyReview and Live. */
    void launch(String metaCampaignRef) {
        if (this.status != AdCampaignStatus.PENDING_POLICY_REVIEW) {
            throw new IllegalStateException(
                "Only a PENDING_POLICY_REVIEW campaign can launch, was: " + this.status);
        }
        this.status = AdCampaignStatus.LIVE;
        this.metaCampaignRef = metaCampaignRef;
        this.updatedAt = Instant.now();
    }

    UUID getCampaignId() {
        return campaignId;
    }

    UUID getPackageId() {
        return packageId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    AdCampaignStatus getStatus() {
        return status;
    }

    String getAudienceDescription() {
        return audienceDescription;
    }

    BigDecimal getBudgetCapAmount() {
        return budgetCapAmount;
    }

    CurrencyCode getBudgetCapCurrency() {
        return budgetCapCurrency;
    }

    Integer getDurationDays() {
        return durationDays;
    }

    String getMetaCampaignRef() {
        return metaCampaignRef;
    }

    BigDecimal getSpendToDateAmount() {
        return spendToDateAmount;
    }

    String getRejectionReason() {
        return rejectionReason;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
