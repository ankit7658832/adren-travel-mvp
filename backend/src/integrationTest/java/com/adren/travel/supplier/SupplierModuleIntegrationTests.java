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
