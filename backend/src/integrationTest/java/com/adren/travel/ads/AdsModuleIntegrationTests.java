package com.adren.travel.ads;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
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
