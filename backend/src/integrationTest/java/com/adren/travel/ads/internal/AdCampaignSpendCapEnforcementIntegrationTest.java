package com.adren.travel.ads.internal;

import com.adren.travel.ads.event.AdCampaignSpendCapReachedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADS-10's own acceptance criterion against real Postgres: a Live
 * campaign's spend reaching its budget cap actually persists the
 * SpendCapReached transition, proving the real repository round-trip —
 * a mocked-repository unit test can't catch a JPA mapping bug here.
 * Lives in {@code ads.internal} to reach the package-private {@link
 * AdCampaignSpendCapEnforcementService} directly, same reasoning as
 * {@code SupplierContentStalenessCheckIntegrationTest}'s own package
 * placement.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"payments", "whitelabel", "supplier", "security", "ai", "booking"})
class AdCampaignSpendCapEnforcementIntegrationTest {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    AdCampaignSpendCapEnforcementService enforcementService;

    @Autowired
    AdCampaignRepository adCampaignRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void aCampaignAlreadyNearItsCapTransitionsToSpendCapReachedAndPublishesTheRealEventADS10(Scenario scenario) {
        // StubMetaAdsClient's mocked increment is always >= 10.00 — seeding
        // exactly 10.00 short of the cap guarantees this single poll
        // crosses/reaches it regardless of the stub's random value,
        // avoiding a flaky test.
        UUID campaignId = seedLiveCampaign(new java.math.BigDecimal("500.00"), new java.math.BigDecimal("490.00"));

        scenario.stimulate(() -> enforcementService.enforceSpendCaps())
            .andWaitForEventOfType(AdCampaignSpendCapReachedEvent.class)
            .matchingMappedValue(AdCampaignSpendCapReachedEvent::campaignId, campaignId)
            .toArrive();

        var row = jdbcTemplate.queryForMap(
            "SELECT status, spend_to_date_amount FROM ad_campaign WHERE campaign_id = ?", campaignId);
        assertThat(row.get("status")).isEqualTo("SPEND_CAP_REACHED");
        assertThat(((Number) row.get("spend_to_date_amount")).doubleValue()).isEqualTo(500.00);
    }

    private UUID seedLiveCampaign(java.math.BigDecimal budgetCapAmount, java.math.BigDecimal spendToDateAmount) {
        UUID campaignId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        java.sql.Timestamp now = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update(
            "INSERT INTO ad_campaign (campaign_id, package_id, consultant_id, status, budget_cap_amount, "
                + "budget_cap_currency, spend_to_date_amount, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, 'LIVE', ?, 'INR', ?, ?, ?, 0)",
            campaignId, UUID.randomUUID(), consultantId, budgetCapAmount, spendToDateAmount, now, now);
        return campaignId;
    }
}
