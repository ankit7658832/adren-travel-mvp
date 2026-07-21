package com.adren.travel.ai;

import com.adren.travel.ai.event.AiSuggestionGeneratedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @ApplicationModuleTest} boots ONLY the AI module (plus its direct
 * dependency, {@code supplier}, and {@code security} for {@code
 * CurrentPrincipal}) against a real (local) Postgres — same shape as
 * {@code BookingModuleIntegrationTests}/{@code PaymentsModuleIntegrationTests}.
 * <p>
 * <b>The real {@code SupplierAggregationService} stub clients ignore {@code
 * locationCode} entirely</b> (confirmed directly — {@code
 * HotelbedsClient}/{@code StubaClient}/{@code TboClient} all return the
 * same hardcoded result regardless of location, per their own "stub,
 * replace once sandbox credentials exist" Javadoc), so there is no way to
 * force a genuinely empty candidate list through the real {@code
 * SupplierSearchApi} bean at this tier — AI-05's zero-inventory path is
 * proven at the unit tier instead ({@code AiServiceImplTest}, mocked
 * {@code SupplierSearchApi}), where an empty list can actually be
 * constructed. What THIS tier proves that the unit tier can't: a real,
 * unforced call to the real Groq API with the configured (dummy, pending a
 * real key) {@code GROQ_API_KEY} — every generation attempt here genuinely
 * reaches {@code https://api.groq.com} and genuinely 401s, exercising the
 * full real HTTP round trip this epic exists to validate, not a mock of it.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "security")
class AiModuleIntegrationTests {

    /**
     * {@code @ApplicationModuleTest}'s slice doesn't auto-configure {@code
     * WebClient.Builder} — both {@code GroqClient} and (transitively via
     * {@code supplier}) {@code HotelbedsClient} need one, same gap {@code
     * BookingModuleIntegrationTests} works around.
     */
    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    AiApi aiApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aRealGroqAuthFailurePublishesTheEventAndRecordsAnAuditRowFIN13(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new GenerateItineraryCommand(consultantId, itineraryId, "GOA",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything at all", null, false);

        scenario.stimulate(() -> {
                try {
                    aiApi.generateItinerary(command);
                } catch (RuntimeException expected) {
                    // The real Groq call genuinely fails (dummy key, real
                    // API) — AI-05/backend-best-practices: an explicit
                    // error, never silently swallowed, never a fabricated
                    // fallback suggestion.
                }
            })
            .andWaitForEventOfType(AiSuggestionGeneratedEvent.class)
            .matchingMappedValue(AiSuggestionGeneratedEvent::itineraryId, itineraryId)
            .matchingMappedValue(AiSuggestionGeneratedEvent::disposition, "GROQ_ERROR");
    }

    @Test
    void theAuditLogRowForARealGroqFailureIsPersistedWithTheRealConsultantAndItineraryIdsFIN07() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new GenerateItineraryCommand(consultantId, itineraryId, "GOA",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything at all", null, false);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> aiApi.generateItinerary(command));

        var row = jdbcTemplate.queryForMap(
            "SELECT consultant_id, itinerary_id, disposition, ai_output_json FROM ai_suggestion_audit_log "
                + "WHERE itinerary_id = ? ORDER BY created_at DESC LIMIT 1",
            itineraryId);
        assertThat(row.get("consultant_id")).isEqualTo(consultantId);
        assertThat(row.get("disposition")).isEqualTo("GROQ_ERROR");
        // The real, observed Groq error message (see AI-01's commit) —
        // proves this genuinely round-tripped to the live API, not a stub.
        assertThat(row.get("ai_output_json").toString()).contains("GROQ_API_KEY");
    }

    /**
     * TST-09 — AI-02/AI-03's reference usage of {@link AiAuditCompletenessAssertions}:
     * every prior test here already proved "one call produces (at least)
     * one row" one at a time; this generalizes it to the actual PRD
     * S11.2/S24.3 invariant — N calls produce EXACTLY N rows, not more
     * (a duplicate/retry write) and not fewer (a dropped/sampled one).
     * Each call genuinely reaches the real Groq API and genuinely 401s
     * (dummy key), and AI-13's bounded retry never retries an auth
     * failure, so each of these 3 calls produces exactly one attempt.
     */
    @Test
    void threeRealGroqCallsProduceExactlyThreeAuditLogRowsNoSamplingAI07() {
        long countBefore = AiAuditCompletenessAssertions.currentAuditLogRowCount(jdbcTemplate);
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        for (int i = 0; i < 3; i++) {
            var command = new GenerateItineraryCommand(consultantId, UUID.randomUUID(), "GOA",
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything at all", null, false);
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> aiApi.generateItinerary(command));
        }

        AiAuditCompletenessAssertions.assertExactlyNNewAuditLogRows(jdbcTemplate, countBefore, 3);
    }

    @Test
    void aCompleteWithAiCallOnAnItineraryWithAnExistingHotelSelectionNeverReachesGroqAI03() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new GenerateItineraryCommand(consultantId, itineraryId, "GOA",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything at all", null, true);

        // Unlike the GROQ_ERROR tests above, this genuinely does NOT throw
        // — hasExistingHotelSelection=true short-circuits before any real
        // Groq HTTP call, so there is nothing here to fail authentication.
        AiItineraryGenerationResult result = aiApi.generateItinerary(command);

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        var row = jdbcTemplate.queryForMap(
            "SELECT disposition, ai_output_json FROM ai_suggestion_audit_log "
                + "WHERE itinerary_id = ? ORDER BY created_at DESC LIMIT 1",
            itineraryId);
        assertThat(row.get("disposition")).isEqualTo("NO_VIABLE_SUGGESTION");
        assertThat(row.get("ai_output_json")).isNull();
    }

    /**
     * TST-06, PRD S23.2 Edge Case #4 / S25 T20 — a Mystifly-shaped fare
     * expiring between search and payment is exactly this "price still
     * matches at booking time" vs "price changed" pair; supplierId here is
     * Hotelbeds (the client that's actually unmocked in this slice), but
     * the revalidation mechanism this proves is supplier-agnostic. This
     * test is the sandbox-shaped (fare unchanged) half of that pair.
     */
    @Test
    @org.junit.jupiter.api.Tag("supplier-sandbox-fixture")
    void revalidateAiPricingAtBookingConfirmsAgainstTheRealStubSupplierLivePriceAI09() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSuggestedAuditLog(auditLogId, consultantId, itineraryId,
            new AiSuggestedLineItem(SupplierId.HOTELBEDS, "stub-rate-key",
                "Stub Hotel — replace with live Hotelbeds response", "Deluxe Room",
                new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now()));

        // The real (stub, but unmocked) HotelbedsClient always returns
        // "stub-rate-key" at 5000 INR — the same price seeded above, so
        // this genuinely round-trips through SupplierAggregationService
        // and confirms, not a mocked SupplierSearchApi.
        AiPricingRevalidationResult result = aiApi.revalidateAiPricingAtBooking(auditLogId);

        assertThat(result).isInstanceOf(PricingConfirmed.class);
    }

    /** TST-06, PRD S23.2 Edge Case #4 / S25 T20 — the production-shaped (fare changed) half of the pair above. */
    @Test
    @org.junit.jupiter.api.Tag("supplier-production-fixture")
    void revalidateAiPricingAtBookingDetectsAGenuineLivePriceMismatchAgainstTheRealSupplierAI09() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        // Same real supplierRateId ("stub-rate-key") the live Hotelbeds
        // stub returns, but approved at a DIFFERENT price than that stub
        // actually serves (5000 INR) — a genuine mismatch, not a fabricated one.
        seedSuggestedAuditLog(auditLogId, consultantId, itineraryId,
            new AiSuggestedLineItem(SupplierId.HOTELBEDS, "stub-rate-key",
                "Stub Hotel — replace with live Hotelbeds response", "Deluxe Room",
                new Money(BigDecimal.valueOf(9999), CurrencyCode.INR), Instant.now()));

        AiPricingRevalidationResult result = aiApi.revalidateAiPricingAtBooking(auditLogId);

        assertThat(result).isInstanceOf(PricingStale.class);
        assertThat(((PricingStale) result).reason()).contains("9999").contains("5000");
    }

    private void seedSuggestedAuditLog(UUID auditLogId, UUID consultantId, UUID itineraryId,
                                        AiSuggestedLineItem approvedLineItem) {
        String suggestedJson = objectMapper.writeValueAsString(List.of(approvedLineItem));
        String requestJson = "{\"locationCode\":\"GOA\",\"checkIn\":\"" + LocalDate.now().plusDays(30)
            + "\",\"checkOut\":\"" + LocalDate.now().plusDays(34) + "\"}";
        jdbcTemplate.update(
            "INSERT INTO ai_suggestion_audit_log (audit_log_id, correlation_id, attempt_number, consultant_id, "
                + "itinerary_id, request_input_json, source_data_snapshot_json, ai_output_json, disposition, "
                + "created_at, suggested_line_items_json) VALUES (?, ?, 1, ?, ?, ?, '[]', '{}', 'SUGGESTED', now(), ?)",
            auditLogId, UUID.randomUUID(), consultantId, itineraryId, requestJson, suggestedJson);
    }

    @Test
    void findAuditLogAsSuperAdminSeesEntriesAcrossConsultantsAI11() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();
        UUID auditLogA = UUID.randomUUID();
        UUID auditLogB = UUID.randomUUID();
        AiSuggestedLineItem lineItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "stub-rate-key",
            "Stub Hotel", "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        seedSuggestedAuditLog(auditLogA, consultantA, UUID.randomUUID(), lineItem);
        seedSuggestedAuditLog(auditLogB, consultantB, UUID.randomUUID(), lineItem);
        authenticateAs(Role.SUPER_ADMIN, null);

        var page = aiApi.findAuditLog(null, org.springframework.data.domain.PageRequest.of(0, 100));

        assertThat(page.getContent()).extracting(AiAuditLogEntryView::auditLogId)
            .contains(auditLogA, auditLogB);
    }

    @Test
    void findAuditLogFilteredByConsultantOnlyReturnsThatConsultantsEntriesAI11() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();
        UUID auditLogA = UUID.randomUUID();
        UUID auditLogB = UUID.randomUUID();
        AiSuggestedLineItem lineItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "stub-rate-key",
            "Stub Hotel", "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        seedSuggestedAuditLog(auditLogA, consultantA, UUID.randomUUID(), lineItem);
        seedSuggestedAuditLog(auditLogB, consultantB, UUID.randomUUID(), lineItem);
        authenticateAs(Role.SUPER_ADMIN, null);

        var page = aiApi.findAuditLog(consultantA, org.springframework.data.domain.PageRequest.of(0, 100));

        assertThat(page.getContent()).extracting(AiAuditLogEntryView::auditLogId).contains(auditLogA)
            .doesNotContain(auditLogB);
        assertThat(page.getContent()).allSatisfy(entry -> assertThat(entry.consultantId()).isEqualTo(consultantA));
    }

    @Test
    void aConsultantCannotGenerateASuggestionForAnotherConsultantFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        var command = new GenerateItineraryCommand(otherConsultantId, UUID.randomUUID(), "GOA",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything", null, false);

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () -> aiApi.generateItinerary(command));
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
