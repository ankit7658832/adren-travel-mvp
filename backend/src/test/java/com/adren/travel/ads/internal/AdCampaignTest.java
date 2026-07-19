package com.adren.travel.ads.internal;

import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ADS-02 — the AdCampaign entity's own state machine, unit-tested directly (backend-best-practices §1, mirroring {@code ItineraryTest}'s shape). */
class AdCampaignTest {

    private AdCampaign newCampaign() {
        return new AdCampaign(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CurrencyCode.INR);
    }

    @Test
    void createStartsInPendingApprovalADS02() {
        AdCampaign campaign = newCampaign();

        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.PENDING_APPROVAL);
    }

    @Test
    void theHappyPathReachesLiveADS02() {
        AdCampaign campaign = newCampaign();

        campaign.submitForPolicyReview();
        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.PENDING_POLICY_REVIEW);

        campaign.launch("meta-ref-123");
        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.LIVE);
        assertThat(campaign.getMetaCampaignRef()).isEqualTo("meta-ref-123");
    }

    @Test
    void rejectPolicyReviewStoresTheReasonAndTransitionsToRejectedADS02() {
        AdCampaign campaign = newCampaign();
        campaign.submitForPolicyReview();

        campaign.rejectPolicyReview("Brand safety concern: unverified claim in headline");

        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.REJECTED);
        assertThat(campaign.getRejectionReason()).isEqualTo("Brand safety concern: unverified claim in headline");
    }

    @Test
    void aRejectedCampaignCannotLaunchADS02() {
        // The exact scenario ADS-02's own acceptance criterion names.
        AdCampaign campaign = newCampaign();
        campaign.submitForPolicyReview();
        campaign.rejectPolicyReview("Rejected");

        assertThatThrownBy(() -> campaign.launch("meta-ref-should-never-be-set"))
            .isInstanceOf(IllegalStateException.class);
        assertThat(campaign.getMetaCampaignRef()).isNull();
    }

    @Test
    void submitForPolicyReviewIsOnlyValidFromPendingApprovalADS02() {
        AdCampaign campaign = newCampaign();
        campaign.submitForPolicyReview();

        assertThatThrownBy(campaign::submitForPolicyReview).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void launchIsOnlyValidFromPendingPolicyReviewADS02() {
        AdCampaign campaign = newCampaign();

        assertThatThrownBy(() -> campaign.launch("too-early")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectPolicyReviewIsOnlyValidFromPendingPolicyReviewADS02() {
        AdCampaign campaign = newCampaign();

        assertThatThrownBy(() -> campaign.rejectPolicyReview("too-early")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitCampaignInputsSetsAllThreeFieldsWhilePendingApprovalADS03() {
        AdCampaign campaign = newCampaign();

        campaign.submitCampaignInputs("Adults 25-45 interested in beach travel", new java.math.BigDecimal("500.00"), 14);

        assertThat(campaign.getAudienceDescription()).isEqualTo("Adults 25-45 interested in beach travel");
        assertThat(campaign.getBudgetCapAmount()).isEqualByComparingTo("500.00");
        assertThat(campaign.getDurationDays()).isEqualTo(14);
    }

    @Test
    void submitCampaignInputsIsOnlyValidWhilePendingApprovalADS03() {
        AdCampaign campaign = newCampaign();
        campaign.submitForPolicyReview();

        assertThatThrownBy(() -> campaign.submitCampaignInputs("audience", new java.math.BigDecimal("500.00"), 14))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordPerformanceSnapshotAccumulatesOntoTheRunningTotalADS09() {
        AdCampaign campaign = newCampaign();
        campaign.submitForPolicyReview();
        campaign.launch("meta-ref-123");

        campaign.recordPerformanceSnapshot(100, 10, 1);
        campaign.recordPerformanceSnapshot(50, 5, 0);

        assertThat(campaign.getImpressions()).isEqualTo(150);
        assertThat(campaign.getClicks()).isEqualTo(15);
        assertThat(campaign.getBookingsAttributed()).isEqualTo(1);
    }

    @Test
    void recordPerformanceSnapshotIsOnlyValidWhileLiveADS09() {
        AdCampaign campaign = newCampaign();

        assertThatThrownBy(() -> campaign.recordPerformanceSnapshot(100, 10, 1))
            .isInstanceOf(IllegalStateException.class);
    }

    private AdCampaign newLiveCampaignWithBudget(String budgetCapAmount) {
        AdCampaign campaign = newCampaign();
        campaign.submitCampaignInputs("Adults 25-45", new java.math.BigDecimal(budgetCapAmount), 14);
        campaign.submitForPolicyReview();
        campaign.launch("meta-ref-123");
        return campaign;
    }

    @Test
    void recordSpendAccumulatesBelowTheCapADS10() {
        AdCampaign campaign = newLiveCampaignWithBudget("500.00");

        campaign.recordSpend(new java.math.BigDecimal("100.00"));
        campaign.recordSpend(new java.math.BigDecimal("50.00"));

        assertThat(campaign.getSpendToDateAmount()).isEqualByComparingTo("150.00");
        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.LIVE);
    }

    @Test
    void recordSpendCapsExactlyAtBudgetAndTransitionsToSpendCapReachedADS10() {
        // PRD §24.6 NFR: must not meaningfully overshoot — an increment
        // that would push spend past the cap is truncated to land exactly
        // on the cap, never beyond it.
        AdCampaign campaign = newLiveCampaignWithBudget("500.00");
        campaign.recordSpend(new java.math.BigDecimal("480.00"));

        campaign.recordSpend(new java.math.BigDecimal("50.00"));

        assertThat(campaign.getSpendToDateAmount()).isEqualByComparingTo("500.00");
        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.SPEND_CAP_REACHED);
    }

    @Test
    void recordSpendIsOnlyValidWhileLiveADS10() {
        AdCampaign campaign = newCampaign();

        assertThatThrownBy(() -> campaign.recordSpend(new java.math.BigDecimal("10.00")))
            .isInstanceOf(IllegalStateException.class);
    }
}
