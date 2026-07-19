package com.adren.travel.ads;

import com.adren.travel.ads.event.AdCampaignCreatedEvent;
import com.adren.travel.ads.event.AdCampaignInputsSubmittedEvent;
import com.adren.travel.ads.event.AdCampaignMetaSuspendedEvent;
import com.adren.travel.ads.event.AdCampaignPausedEvent;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ApplicationModuleTest} boots the Ads module plus its two direct
 * dependencies, {@code booking} and {@code ai}, against a real (local)
 * Postgres — same shape as {@code BookingModuleIntegrationTests}/{@code
 * AiModuleIntegrationTests}. {@code extraIncludes} widens one hop further
 * than {@code DIRECT_DEPENDENCIES} covers on its own: {@code booking}'s own
 * constructor dependencies on {@code payments}/{@code whitelabel}/{@code
 * supplier}, and {@code whitelabel}'s own dependency on {@code security}
 * (same second-degree-edge reasoning {@code BookingModuleIntegrationTests}
 * documents).
 * <p>
 * Package rows are seeded directly via SQL (bypassing the full
 * DRAFT-itinerary-to-PUBLISHED-package booking pipeline {@code
 * BookingModuleIntegrationTests} already covers) — what THIS tier proves is
 * the cross-module wiring itself: {@code ads.AdsServiceImpl} resolving a
 * real Package's content via the real {@code BookingApi} and passing it
 * into the real {@code AiApi}, reaching the real (dummy-keyed) Groq API,
 * same "genuine unforced failure" pattern as {@code AiModuleIntegrationTests}.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"payments", "whitelabel", "supplier", "security"})
class AdsModuleIntegrationTests {

    /** {@code @ApplicationModuleTest}'s slice doesn't auto-configure {@code WebClient.Builder} — same gap {@code AiModuleIntegrationTests} works around. */
    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    AdsApi adsApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // Field injection is fine for this test's own module's Api (AdsApi
    // above), but Spring Modulith's architecture check flags field
    // injection of ANOTHER module's type from outside it — BookingApi is
    // booking's own, so constructor injection here, same reasoning as
    // StripeWebhookConfirmsBookingEndToEndIT's identical fix.
    final BookingApi bookingApi;
    final WhitelabelApi whitelabelApi;

    @Autowired
    AdsModuleIntegrationTests(BookingApi bookingApi, WhitelabelApi whitelabelApi) {
        this.bookingApi = bookingApi;
        this.whitelabelApi = whitelabelApi;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateAdCreativeForPackageThrowsForAnUnknownPackageAI12() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        var command = new GenerateAdCreativeForPackageCommand(UUID.randomUUID(), 2);

        assertThatThrownBy(() -> adsApi.generateAdCreativeForPackage(command))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateAdCreativeForPackageThrowsForAnUnpublishedDraftPackageAI12() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "DRAFT");
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new GenerateAdCreativeForPackageCommand(packageId, 2);

        assertThatThrownBy(() -> adsApi.generateAdCreativeForPackage(command))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateAdCreativeForPackageCannotBeCalledForAnotherConsultantsPackageAI12() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID packageId = seedPackage(ownerConsultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        var command = new GenerateAdCreativeForPackageCommand(packageId, 2);

        assertThatThrownBy(() -> adsApi.generateAdCreativeForPackage(command))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generateAdCreativeForPackageReachesTheRealGroqApiAndRecordsAnAuditRowWithTheRealPackageContentAI12() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new GenerateAdCreativeForPackageCommand(packageId, 2);

        // The real (dummy-keyed) Groq call genuinely 401s — AI-05/AI-12,
        // backend-best-practices: an explicit error, never a silently
        // fabricated fallback ad.
        assertThatThrownBy(() -> adsApi.generateAdCreativeForPackage(command))
            .isInstanceOf(RuntimeException.class);

        var row = jdbcTemplate.queryForMap(
            "SELECT consultant_id, package_id, disposition, source_data_snapshot_json FROM ad_creative_audit_log "
                + "WHERE package_id = ? ORDER BY created_at DESC LIMIT 1",
            packageId);
        assertThat(row.get("consultant_id")).isEqualTo(consultantId);
        assertThat(row.get("disposition")).isEqualTo("GROQ_ERROR");
        // The real Package content reached the audit trail — not a
        // fabricated or cached copy.
        assertThat(row.get("source_data_snapshot_json").toString()).contains("Goa Beach Escape");
    }

    @Test
    void provisionAdAccountCreatesARealRowAndIsIdempotentOnASecondCallADS01() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.SUPER_ADMIN, null);

        AdAccountView first = adsApi.provisionAdAccount(consultantId);
        AdAccountView second = adsApi.provisionAdAccount(consultantId);

        assertThat(second.adAccountId()).isEqualTo(first.adAccountId());
        assertThat(second.metaBusinessManagerId()).isEqualTo(first.metaBusinessManagerId());
        Long rowCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM ad_account WHERE consultant_id = ?", Long.class, consultantId);
        assertThat(rowCount).isEqualTo(1L);
    }

    @Test
    void provisionAdAccountRejectsANonSuperAdminCallerADS01() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> adsApi.provisionAdAccount(UUID.randomUUID()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void createCampaignPersistsAtPendingApprovalAndPublishesTheRealEventADS02(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new CreateCampaignCommand(packageId);

        scenario.stimulate(() -> adsApi.createCampaign(command))
            .andWaitForEventOfType(AdCampaignCreatedEvent.class)
            .matchingMappedValue(AdCampaignCreatedEvent::packageId, packageId)
            .toArrive();

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM ad_campaign WHERE package_id = ?", String.class, packageId);
        assertThat(status).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void submitCampaignInputsPersistsAllThreeFieldsAgainstARealCampaignADS03(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        var command = new SubmitCampaignInputsCommand(
            created.campaignId(), "Adults 25-45 interested in beach travel", new java.math.BigDecimal("500.00"), 14);

        scenario.stimulate(() -> adsApi.submitCampaignInputs(command))
            .andWaitForEventOfType(AdCampaignInputsSubmittedEvent.class)
            .matchingMappedValue(AdCampaignInputsSubmittedEvent::campaignId, created.campaignId())
            .toArrive();

        var row = jdbcTemplate.queryForMap(
            "SELECT audience_description, budget_cap_amount, duration_days FROM ad_campaign WHERE campaign_id = ?",
            created.campaignId());
        assertThat(row.get("audience_description")).isEqualTo("Adults 25-45 interested in beach travel");
        assertThat(((Number) row.get("budget_cap_amount")).doubleValue()).isEqualTo(500.00);
        assertThat(row.get("duration_days")).isEqualTo(14);
    }

    @Test
    void submitCampaignInputsCannotBeCalledForAnotherConsultantsCampaignADS03() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID packageId = seedPackage(ownerConsultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));

        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        var command = new SubmitCampaignInputsCommand(created.campaignId(), "audience", new java.math.BigDecimal("500.00"), 14);

        assertThatThrownBy(() -> adsApi.submitCampaignInputs(command))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generateCreativeForCampaignPropagatesTheRealGroqFailureAndPersistsNothingADS04() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));

        // Same real (dummy-keyed) Groq 401 as generateAdCreativeForPackage's
        // own coverage — nothing new to mock, this proves the campaign-scoped
        // wrapper propagates the same failure rather than swallowing it.
        assertThatThrownBy(() -> adsApi.generateCreativeForCampaign(created.campaignId(), 2))
            .isInstanceOf(RuntimeException.class);

        Long variantCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM ad_campaign_creative_variant WHERE campaign_id = ?", Long.class, created.campaignId());
        assertThat(variantCount).isZero();
    }

    @Test
    void findCreativeVariantsForCampaignReturnsRealPersistedVariantsADS04() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        UUID variantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', false)",
            variantId, created.campaignId());

        var variants = adsApi.findCreativeVariantsForCampaign(created.campaignId());

        assertThat(variants).hasSize(1);
        assertThat(variants.get(0).variantId()).isEqualTo(variantId);
        assertThat(variants.get(0).approved()).isFalse();
    }

    @Test
    void findCreativeVariantsForCampaignRejectsAnotherConsultantADS04() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID packageId = seedPackage(ownerConsultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));

        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> adsApi.findCreativeVariantsForCampaign(created.campaignId()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approveCreativeVariantPersistsTheFlagAndPublishesTheRealEventADS05(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        UUID variantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', false)",
            variantId, created.campaignId());

        scenario.stimulate(() -> adsApi.approveCreativeVariant(created.campaignId(), variantId))
            .andWaitForEventOfType(com.adren.travel.ads.event.AdCampaignCreativeVariantApprovedEvent.class)
            .matchingMappedValue(com.adren.travel.ads.event.AdCampaignCreativeVariantApprovedEvent::variantId, variantId)
            .toArrive();

        Boolean approved = jdbcTemplate.queryForObject(
            "SELECT approved FROM ad_campaign_creative_variant WHERE variant_id = ?", Boolean.class, variantId);
        assertThat(approved).isTrue();
    }

    @Test
    void approveCreativeVariantRejectsAnotherConsultantsCallADS05() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID packageId = seedPackage(ownerConsultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        UUID variantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', false)",
            variantId, created.campaignId());

        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> adsApi.approveCreativeVariant(created.campaignId(), variantId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void submitCampaignForPolicyReviewTransitionsARealCampaignAndAppearsInTheQueueADS06(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        UUID variantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', true)",
            variantId, created.campaignId());

        scenario.stimulate(() -> adsApi.submitCampaignForPolicyReview(created.campaignId()))
            .andWaitForEventOfType(com.adren.travel.ads.event.AdCampaignSubmittedForPolicyReviewEvent.class)
            .matchingMappedValue(com.adren.travel.ads.event.AdCampaignSubmittedForPolicyReviewEvent::campaignId, created.campaignId())
            .toArrive();

        authenticateAs(Role.SUPER_ADMIN, null);
        var queue = adsApi.findCampaignsPendingPolicyReview(org.springframework.data.domain.Pageable.unpaged());
        assertThat(queue.getContent()).extracting(AdCampaignView::campaignId).contains(created.campaignId());
    }

    @Test
    void submitCampaignForPolicyReviewRejectsWhenAVariantIsUnapprovedADS05() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', false)",
            UUID.randomUUID(), created.campaignId());

        assertThatThrownBy(() -> adsApi.submitCampaignForPolicyReview(created.campaignId()))
            .isInstanceOf(IllegalStateException.class);
        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM ad_campaign WHERE campaign_id = ?", String.class, created.campaignId());
        assertThat(status).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void rejectCampaignPolicyReviewTransitionsARealCampaignAndPublishesTheRealEventADS06(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', true)",
            UUID.randomUUID(), created.campaignId());
        adsApi.submitCampaignForPolicyReview(created.campaignId());
        authenticateAs(Role.SUPER_ADMIN, null);

        scenario.stimulate(() -> adsApi.rejectCampaignPolicyReview(created.campaignId(), "Unverified pricing claim"))
            .andWaitForEventOfType(com.adren.travel.ads.event.AdCampaignPolicyReviewRejectedEvent.class)
            .matchingMappedValue(com.adren.travel.ads.event.AdCampaignPolicyReviewRejectedEvent::campaignId, created.campaignId())
            .toArrive();

        var row = jdbcTemplate.queryForMap(
            "SELECT status, rejection_reason FROM ad_campaign WHERE campaign_id = ?", created.campaignId());
        assertThat(row.get("status")).isEqualTo("REJECTED");
        assertThat(row.get("rejection_reason")).isEqualTo("Unverified pricing claim");
    }

    @Test
    void launchCampaignTransitionsARealCampaignToLiveAndStoresTheRealMetaRefADS07(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', true)",
            UUID.randomUUID(), created.campaignId());
        adsApi.submitCampaignForPolicyReview(created.campaignId());
        authenticateAs(Role.SUPER_ADMIN, null);

        scenario.stimulate(() -> adsApi.launchCampaign(created.campaignId()))
            .andWaitForEventOfType(com.adren.travel.ads.event.AdCampaignLaunchedEvent.class)
            .matchingMappedValue(com.adren.travel.ads.event.AdCampaignLaunchedEvent::campaignId, created.campaignId())
            .toArrive();

        var row = jdbcTemplate.queryForMap(
            "SELECT status, meta_campaign_ref FROM ad_campaign WHERE campaign_id = ?", created.campaignId());
        assertThat(row.get("status")).isEqualTo("LIVE");
        assertThat(row.get("meta_campaign_ref")).isNotNull();
    }

    @Test
    void launchCampaignRejectsACampaignThatIsStillPendingApprovalADS07() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        authenticateAs(Role.SUPER_ADMIN, null);

        assertThatThrownBy(() -> adsApi.launchCampaign(created.campaignId()))
            .isInstanceOf(IllegalStateException.class);
        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM ad_campaign WHERE campaign_id = ?", String.class, created.campaignId());
        assertThat(status).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void findCampaignsForConsultantReturnsARealCampaignWithItsPerformanceSnapshotADS09() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "UPDATE ad_campaign SET impressions = 150, clicks = 12, bookings_attributed = 2 WHERE campaign_id = ?",
            created.campaignId());

        var page = adsApi.findCampaignsForConsultant(consultantId, org.springframework.data.domain.Pageable.unpaged());

        assertThat(page.getContent()).extracting(AdCampaignView::campaignId).contains(created.campaignId());
        AdCampaignView withSnapshot = page.getContent().stream()
            .filter(v -> v.campaignId().equals(created.campaignId())).findFirst().orElseThrow();
        assertThat(withSnapshot.impressions()).isEqualTo(150);
        assertThat(withSnapshot.clicks()).isEqualTo(12);
        assertThat(withSnapshot.bookingsAttributed()).isEqualTo(2);
    }

    @Test
    void findCampaignsForConsultantRejectsAnotherConsultantsQueryADS09() {
        UUID ownerConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> adsApi.findCampaignsForConsultant(
            ownerConsultantId, org.springframework.data.domain.Pageable.unpaged()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findCampaignBillingDetailReturnsSpendBudgetAndEveryRealTransactionADS11() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "UPDATE ad_campaign SET spend_to_date_amount = 50.00 WHERE campaign_id = ?", created.campaignId());
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_spend_transaction (transaction_id, campaign_id, amount, recorded_at) "
                + "VALUES (?, ?, 50.00, now())",
            UUID.randomUUID(), created.campaignId());

        var detail = adsApi.findCampaignBillingDetail(created.campaignId());

        assertThat(detail.spendToDateAmount()).isEqualByComparingTo("50.00");
        assertThat(detail.transactions()).hasSize(1);
        assertThat(detail.transactions().get(0).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void findCampaignBillingDetailRejectsAnotherConsultantsQueryADS11() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID packageId = seedPackage(ownerConsultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));

        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> adsApi.findCampaignBillingDetail(created.campaignId()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aPackagePriceChangeAutoPausesItsRealLiveCampaignADS12(Scenario scenario) {
        // updatePackagePrice (unlike every other write path this test class
        // otherwise exercises through AdsApi alone) goes through
        // BookingServiceImpl#requireActiveUnlessSuperAdmin, which requires a
        // REAL, onboarded, ACTIVE consultant row — a bare UUID.randomUUID()
        // (every other test's consultantId) fails whitelabel's own lookup.
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(new OnboardConsultantCommand("Goa Getaways", Market.INDIA,
            Map.of("gstRegistration", "GST123", "businessPan", "PAN123", "bankDetails", "IFSC0001/12345")));
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Escape to Goa', 'Book now', true)",
            UUID.randomUUID(), created.campaignId());
        adsApi.submitCampaignForPolicyReview(created.campaignId());
        authenticateAs(Role.SUPER_ADMIN, null);
        adsApi.launchCampaign(created.campaignId());
        authenticateAs(Role.CONSULTANT, consultantId);

        scenario.stimulate(() -> bookingApi.updatePackagePrice(packageId, new BigDecimal("6000.00")))
            .andWaitForEventOfType(AdCampaignPausedEvent.class)
            .matchingMappedValue(AdCampaignPausedEvent::campaignId, created.campaignId())
            .toArrive();

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM ad_campaign WHERE campaign_id = ?", String.class, created.campaignId());
        assertThat(status).isEqualTo("PAUSED");
    }

    @Test
    void reportMetaAccountSuspensionFlagsEveryRealNonRejectedCampaignForTheConsultantADS13(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID livePackageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        UUID rejectedPackageId = seedPackage(consultantId, "Kerala Backwaters", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView liveCampaign = adsApi.createCampaign(new CreateCampaignCommand(livePackageId));
        AdCampaignView rejectedCampaign = adsApi.createCampaign(new CreateCampaignCommand(rejectedPackageId));
        jdbcTemplate.update("UPDATE ad_campaign SET status = 'REJECTED' WHERE campaign_id = ?", rejectedCampaign.campaignId());
        authenticateAs(Role.SUPER_ADMIN, null);

        scenario.stimulate(() -> adsApi.reportMetaAccountSuspension(consultantId))
            .andWaitForEventOfType(AdCampaignMetaSuspendedEvent.class)
            .matchingMappedValue(AdCampaignMetaSuspendedEvent::campaignId, liveCampaign.campaignId())
            .toArrive();

        Boolean liveSuspended = jdbcTemplate.queryForObject(
            "SELECT meta_suspended FROM ad_campaign WHERE campaign_id = ?", Boolean.class, liveCampaign.campaignId());
        Boolean rejectedSuspended = jdbcTemplate.queryForObject(
            "SELECT meta_suspended FROM ad_campaign WHERE campaign_id = ?", Boolean.class, rejectedCampaign.campaignId());
        assertThat(liveSuspended).isTrue();
        assertThat(rejectedSuspended).isFalse();
    }

    @Test
    void submitCampaignForPolicyReviewFlagsARealPolicyTemplateViolationAgainstTheRealConfiguredPhraseListADS15() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = seedPackage(consultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, consultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));
        jdbcTemplate.update(
            "INSERT INTO ad_campaign_creative_variant (variant_id, campaign_id, headline, body_text, approved) "
                + "VALUES (?, ?, 'Guaranteed lowest price!', 'Book now', true)",
            UUID.randomUUID(), created.campaignId());

        AdCampaignView reviewed = adsApi.submitCampaignForPolicyReview(created.campaignId());

        // Flagged, never blocked — the real application.yml's
        // banned-phrases list includes "guaranteed".
        assertThat(reviewed.status()).isEqualTo("PENDING_POLICY_REVIEW");
        assertThat(reviewed.policyTemplateFlagged()).isTrue();
        assertThat(reviewed.policyTemplateFlagReason()).contains("guaranteed");

        var row = jdbcTemplate.queryForMap(
            "SELECT policy_template_flagged, policy_template_flag_reason FROM ad_campaign WHERE campaign_id = ?",
            created.campaignId());
        assertThat(row.get("policy_template_flagged")).isEqualTo(true);
        assertThat(row.get("policy_template_flag_reason").toString()).contains("guaranteed");
    }

    @Test
    void findCampaignByIdRejectsAnotherConsultantsQueryADS13() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID packageId = seedPackage(ownerConsultantId, "Goa Beach Escape", "PUBLISHED");
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        AdCampaignView created = adsApi.createCampaign(new CreateCampaignCommand(packageId));

        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> adsApi.findCampaignById(created.campaignId())).isInstanceOf(AccessDeniedException.class);
    }

    private UUID seedPackage(UUID consultantId, String name, String status) {
        java.sql.Timestamp now = java.sql.Timestamp.from(Instant.now());
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) "
                + "VALUES (?, ?, 'BOOKED', false, ?, ?)",
            itineraryId, consultantId, now, now);
        UUID packageId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO travel_package (package_id, source_itinerary_id, consultant_id, name, description, "
                + "validity_start, validity_end, base_price, markup_price, currency, max_pax, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'A relaxing beach package', ?, ?, 20000.00, 5000.00, 'INR', 4, ?, ?)",
            packageId, itineraryId, consultantId, name,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), status, now);
        return packageId;
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
