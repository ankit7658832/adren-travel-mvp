package com.adren.travel.dashboard;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
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

/**
 * {@code @ApplicationModuleTest} boots the {@code dashboard} module plus
 * its four direct dependencies ({@code booking}/{@code payments}/{@code
 * ads}/{@code ai}) against a real (local) Postgres — same shape as {@code
 * AdsModuleIntegrationTests}. {@code extraIncludes} widens one hop
 * further for each of those modules' own constructor dependencies
 * ({@code whitelabel}, {@code supplier}, {@code security}), same
 * second-degree-edge reasoning already documented elsewhere.
 * <p>
 * What this tier proves is the cross-module composite-read wiring itself
 * (HRD-09/HRD-11's own AC) — every downstream calculation is already
 * proven at the unit tier in each owning module's own tests.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"whitelabel", "supplier", "security"})
class DashboardModuleIntegrationTests {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    DashboardApi dashboardApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // Field injection is fine for this test's own module's Api
    // (DashboardApi above) and framework types (JdbcTemplate), but Spring
    // Modulith's architecture check flags field injection of ANOTHER
    // module's type from outside it — WhitelabelApi is whitelabel's own,
    // so constructor injection here, same reasoning as
    // StripeWebhookConfirmsBookingEndToEndIT's identical fix.
    final WhitelabelApi whitelabelApi;

    @Autowired
    DashboardModuleIntegrationTests(WhitelabelApi whitelabelApi) {
        this.whitelabelApi = whitelabelApi;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findConsultantDashboardAggregatesRealDataAcrossBookingPaymentsAndAdsHRD09() {
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(new OnboardConsultantCommand("Goa Getaways", Market.INDIA,
            Map.of("gstRegistration", "GST123", "businessPan", "PAN123", "bankDetails", "IFSC0001/12345"),
            "owner-" + UUID.randomUUID() + "@example.com", "InitialPassword1!"));
        UUID sourceItineraryId = seedBookedItineraryPackageAndBooking(consultantId, "Goa Beach Escape");

        authenticateAs(Role.CONSULTANT, consultantId);
        ConsultantDashboardView view = dashboardApi.findConsultantDashboard(consultantId);

        assertThat(view.wallet().consultantId()).isEqualTo(consultantId);
        assertThat(view.metrics().bookingsThisMonth()).isEqualTo(1);
        assertThat(view.metrics().gmvThisMonth().amount()).isEqualByComparingTo("20000.00");
        assertThat(view.topPackages()).hasSize(1);
        assertThat(view.topPackages().get(0).bookingCount()).isEqualTo(1);
        assertThat(view.pendingQuotations()).isEmpty();
        assertThat(view.activeCampaigns()).isEmpty();
    }

    @Test
    void findConsultantDashboardRejectsAnotherConsultantsQueryHRD09() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () -> dashboardApi.findConsultantDashboard(UUID.randomUUID()));
    }

    @Test
    void findSuperAdminDashboardRunsAgainstRealPostgresAndReturnsAValidShapeHRD11() {
        authenticateAs(Role.SUPER_ADMIN, null);

        SuperAdminDashboardView view = dashboardApi.findSuperAdminDashboard();

        assertThat(view.supplierPerformance()).hasSize(SupplierId.values().length);
        assertThat(view.aiGovernanceSummary()).isNotNull();
        assertThat(view.gmv()).isNotNull();
        assertThat(view.adSpend()).isNotNull();
    }

    @Test
    void findSuperAdminDashboardRejectsANonSuperAdminCallerHRD11() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            dashboardApi::findSuperAdminDashboard);
    }

    private UUID seedBookedItineraryPackageAndBooking(UUID consultantId, String packageName) {
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
                + "VALUES (?, ?, ?, ?, 'A relaxing beach package', ?, ?, 18000.00, 2000.00, 'INR', 4, 'PUBLISHED', ?)",
            packageId, itineraryId, consultantId, packageName,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), now);
        UUID bookingId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO booking (booking_id, itinerary_id, consultant_id, status, total_sell_price, "
                + "total_sell_currency, payment_method, pnr_searchable_ref, created_at) "
                + "VALUES (?, ?, ?, 'CONFIRMED', 20000.00, 'INR', 'WALLET', ?, ?)",
            bookingId, itineraryId, consultantId, "PNR" + bookingId.toString().substring(0, 8).toUpperCase(), now);
        return itineraryId;
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
