package com.adren.travel.ai;

import com.adren.travel.ai.event.AiSuggestionGeneratedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
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
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything at all", null);

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
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything at all", null);

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

    @Test
    void aConsultantCannotGenerateASuggestionForAnotherConsultantFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        var command = new GenerateItineraryCommand(otherConsultantId, UUID.randomUUID(), "GOA",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything", null);

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
