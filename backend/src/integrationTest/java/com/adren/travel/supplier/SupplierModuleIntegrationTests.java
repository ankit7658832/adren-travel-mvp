package com.adren.travel.supplier;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ApplicationModuleTest} boots ONLY the Supplier module (plus
 * {@code security} for {@code CurrentPrincipal}) against a real (local)
 * Postgres — same shape as {@code AiModuleIntegrationTests}.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "security")
class SupplierModuleIntegrationTests {

    /** Same {@code WebClient.Builder} slice gap {@code AiModuleIntegrationTests} works around — {@code HotelbedsClient}/{@code StubaClient}/{@code TboClient} all need one. */
    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    SupplierSearchApi supplierSearchApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitLocalDmcPersistsAsPendingWithTheRealConsultantIdDMC01() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER", "ACTIVITY"), "City tour from 2000 INR", "Ref: partner@example.com"));

        var row = jdbcTemplate.queryForMap(
            "SELECT consultant_id, status, business_name FROM local_dmc_record WHERE local_dmc_id = ?", localDmcId);
        assertThat(row.get("consultant_id")).isEqualTo(consultantId);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("business_name")).isEqualTo("Goa Local Tours");
    }

    @Test
    void activateLocalDmcWithoutVerificationNotesIsRejectedAndStatusStaysPendingDMC02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "City tour", "Ref"));

        assertThatThrownBy(() -> supplierSearchApi.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand(null)))
            .isInstanceOf(LocalDmcVerificationRequiredException.class);

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM local_dmc_record WHERE local_dmc_id = ?", String.class, localDmcId);
        assertThat(status).isEqualTo("PENDING");
    }

    @Test
    void activateLocalDmcWithVerificationNotesTransitionsToActiveDMC02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "City tour", "Ref"));

        supplierSearchApi.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand("Business license verified."));

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM local_dmc_record WHERE local_dmc_id = ?", String.class, localDmcId);
        assertThat(status).isEqualTo("ACTIVE");
    }

    @Test
    void aConsultantCannotActivateAnotherConsultantsLocalDmcFND03() {
        UUID ownerConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "City tour", "Ref"));
        SecurityContextHolder.clearContext();

        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> supplierSearchApi.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand("Checked.")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void bulkUploadPersistsEveryRowOfAFullyValidCsvDMC03() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "x", "y"));
        String csv = "productName,category,netRate,netRateCurrency,cancellationPolicyText,availableFrom,availableTo\n"
            + "City Tour,ACTIVITY,2000,INR,\"Free cancellation, up to 24 hours before\",2026-08-01,2026-12-31\n"
            + "Airport Transfer,TRANSFER,1500,INR,No refunds,2026-08-01,2026-12-31\n";

        var result = supplierSearchApi.bulkUploadLocalDmcInventory(localDmcId, csv);

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
        Long persistedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM local_dmc_inventory_item WHERE local_dmc_id = ?", Long.class, localDmcId);
        assertThat(persistedCount).isEqualTo(2);
    }

    @Test
    void bulkUploadWithAnInvalidRowPersistsNothingAtAllDMC03() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "x", "y"));
        String csv = "productName,category,netRate,netRateCurrency,cancellationPolicyText,availableFrom,availableTo\n"
            + "City Tour,ACTIVITY,2000,INR,Free cancellation,2026-08-01,2026-12-31\n"
            + "Bad Row,ACTIVITY,not-a-number,INR,Free cancellation,2026-08-01,2026-12-31\n";

        var result = supplierSearchApi.bulkUploadLocalDmcInventory(localDmcId, csv);

        assertThat(result.successCount()).isZero();
        assertThat(result.errors()).hasSize(1);
        Long persistedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM local_dmc_inventory_item WHERE local_dmc_id = ?", Long.class, localDmcId);
        assertThat(persistedCount).isZero();
    }

    @Test
    void recordingBookingsAndACancellationRecalculatesTheRollingRateDMC04() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "x", "y"));
        SecurityContextHolder.clearContext();
        authenticateAs(Role.SUPER_ADMIN, null);

        supplierSearchApi.recordLocalDmcBooking(localDmcId);
        supplierSearchApi.recordLocalDmcBooking(localDmcId);
        supplierSearchApi.recordLocalDmcBooking(localDmcId);
        supplierSearchApi.recordLocalDmcBooking(localDmcId);
        supplierSearchApi.recordLocalDmcCancellation(localDmcId);

        var row = jdbcTemplate.queryForMap(
            "SELECT total_bookings_count, cancelled_bookings_count, cancellation_rate FROM local_dmc_record WHERE local_dmc_id = ?",
            localDmcId);
        assertThat(row.get("total_bookings_count")).isEqualTo(4);
        assertThat(row.get("cancelled_bookings_count")).isEqualTo(1);
        assertThat(((java.math.BigDecimal) row.get("cancellation_rate"))).isEqualByComparingTo("0.2500");
    }

    @Test
    void exceedingTheCancellationRateThresholdFlagsTheRealPersistedRecordDMC05() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "x", "y"));
        SecurityContextHolder.clearContext();
        authenticateAs(Role.SUPER_ADMIN, null);

        // Default threshold (application.yml) is 20% — one booking, one
        // cancellation is 100%, well past it.
        supplierSearchApi.recordLocalDmcBooking(localDmcId);
        supplierSearchApi.recordLocalDmcCancellation(localDmcId);

        Boolean flagged = jdbcTemplate.queryForObject(
            "SELECT flagged FROM local_dmc_record WHERE local_dmc_id = ?", Boolean.class, localDmcId);
        assertThat(flagged).isTrue();

        // Visible via the real findLocalDmcs read path too, not just the raw column.
        var page = supplierSearchApi.findLocalDmcs(consultantId, PageRequest.of(0, 20));
        assertThat(page.getContent()).extracting(LocalDmcView::flagged).containsExactly(true);
    }

    @Test
    void updateLocalDmcInventoryItemPersistsTheEditsAndIsReflectedOnRereadDMC10() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "x", "y"));
        String csv = "productName,category,netRate,netRateCurrency,cancellationPolicyText,availableFrom,availableTo\n"
            + "City Tour,ACTIVITY,2000,INR,Free cancellation,2026-08-01,2026-12-31\n";
        supplierSearchApi.bulkUploadLocalDmcInventory(localDmcId, csv);
        UUID itemId = jdbcTemplate.queryForObject(
            "SELECT item_id FROM local_dmc_inventory_item WHERE local_dmc_id = ?", UUID.class, localDmcId);

        supplierSearchApi.updateLocalDmcInventoryItem(localDmcId, itemId, new LocalDmcInventoryItemCommand(
            "City Tour (revised)", com.adren.travel.shared.ProductCategory.ACTIVITY, new java.math.BigDecimal("2500"),
            com.adren.travel.shared.CurrencyCode.INR, "No refunds",
            java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2027, 1, 31)));

        var row = jdbcTemplate.queryForMap(
            "SELECT product_name, net_rate, cancellation_policy_text FROM local_dmc_inventory_item WHERE item_id = ?", itemId);
        assertThat(row.get("product_name")).isEqualTo("City Tour (revised)");
        assertThat(((java.math.BigDecimal) row.get("net_rate"))).isEqualByComparingTo("2500");
        assertThat(row.get("cancellation_policy_text")).isEqualTo("No refunds");

        var page = supplierSearchApi.findLocalDmcInventory(localDmcId, PageRequest.of(0, 20));
        assertThat(page.getContent()).extracting(LocalDmcInventoryItemView::productName).containsExactly("City Tour (revised)");
    }

    @Test
    void aConsultantCannotUpdateAnotherConsultantsInventoryItemFND03() {
        UUID ownerConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownerConsultantId);
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            "Goa Local Tours", List.of("TRANSFER"), "x", "y"));
        String csv = "productName,category,netRate,netRateCurrency,cancellationPolicyText,availableFrom,availableTo\n"
            + "City Tour,ACTIVITY,2000,INR,Free cancellation,2026-08-01,2026-12-31\n";
        supplierSearchApi.bulkUploadLocalDmcInventory(localDmcId, csv);
        UUID itemId = jdbcTemplate.queryForObject(
            "SELECT item_id FROM local_dmc_inventory_item WHERE local_dmc_id = ?", UUID.class, localDmcId);
        SecurityContextHolder.clearContext();
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> supplierSearchApi.updateLocalDmcInventoryItem(localDmcId, itemId,
            new LocalDmcInventoryItemCommand("Hacked", com.adren.travel.shared.ProductCategory.ACTIVITY,
                new java.math.BigDecimal("1"), com.adren.travel.shared.CurrencyCode.INR, "x",
                java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 12, 31))))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findLocalDmcsScopesAConsultantToTheirOwnRecordsOnlyFND03() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantA);
        supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand("A's DMC", List.of("TRANSFER"), "x", "y"));
        SecurityContextHolder.clearContext();
        authenticateAs(Role.CONSULTANT, consultantB);
        supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand("B's DMC", List.of("TRANSFER"), "x", "y"));
        SecurityContextHolder.clearContext();

        authenticateAs(Role.CONSULTANT, consultantA);
        var page = supplierSearchApi.findLocalDmcs(null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(LocalDmcView::businessName).containsExactly("A's DMC");
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
