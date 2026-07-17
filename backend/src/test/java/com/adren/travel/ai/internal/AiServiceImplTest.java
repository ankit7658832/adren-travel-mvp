package com.adren.travel.ai.internal;

import com.adren.travel.ai.AiItineraryGenerationResult;
import com.adren.travel.ai.AiItinerarySuggestion;
import com.adren.travel.ai.GenerateItineraryCommand;
import com.adren.travel.ai.NoViableSuggestion;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI-02's core acceptance criterion (grounded generation only), AI-04
 * (supplierId/availabilityAsOf as first-class fields), AI-05 (explicit
 * no-viable-suggestion state), and AI-07 (audit-log-as-transactional-gate)
 * — all genuinely co-designed in this one service method, so tested
 * together here rather than artificially separated per story ID (see the
 * commit history for which AC each test proves for which story).
 */
@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    GroqClient groqClient;

    @Mock
    SupplierSearchApi supplierSearchApi;

    @Mock
    AiSuggestionAuditLogRecorder auditLogRecorder;

    @Mock
    org.springframework.context.ApplicationEventPublisher events;

    AiServiceImpl service;

    private static final SupplierSearchResult TAJ = new SupplierSearchResult(
        SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace", "Deluxe Room",
        new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.5);
    private static final SupplierSearchResult OBEROI = new SupplierSearchResult(
        SupplierId.STUBA, "rate-oberoi-1", "The Oberoi", "Luxury Suite",
        new Money(BigDecimal.valueOf(15000), CurrencyCode.INR), 4.9);

    @BeforeEach
    void setUp() {
        service = new AiServiceImpl(groqClient, supplierSearchApi, auditLogRecorder, new ObjectMapper(), events);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAGroundedSuggestionWhenGroqSelectsARealCandidateFIN02() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels("GOA", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5)))
            .thenReturn(List.of(TAJ, OBEROI));
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean()))
            .thenReturn("{\"selectedSupplierRateIds\":[\"rate-taj-1\"],\"viable\":true,\"reason\":null}");

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, itineraryId, "GOA", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
            "A relaxing beach trip", null));

        assertThat(result).isInstanceOf(AiItinerarySuggestion.class);
        AiItinerarySuggestion suggestion = (AiItinerarySuggestion) result;
        assertThat(suggestion.lineItems()).hasSize(1);
        // AI-04: supplierId and availabilityAsOf are first-class fields on every suggested line item.
        assertThat(suggestion.lineItems().get(0).supplierId()).isEqualTo(SupplierId.HOTELBEDS);
        assertThat(suggestion.lineItems().get(0).availabilityAsOf()).isNotNull();
        assertThat(suggestion.lineItems().get(0).propertyName()).isEqualTo("Taj Palace");

        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.SUGGESTED);
        assertThat(captor.getValue().getAuditLogId()).isEqualTo(suggestion.auditLogId());
    }

    @Test
    void statesInabilityExplicitlyWhenZeroInventoryExistsFIN05() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of());

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "NOWHERE", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything goes", null));

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        assertThat(((NoViableSuggestion) result).reason()).contains("No inventory available");
        verify(groqClient, org.mockito.Mockito.never()).chatCompletion(any(), any(), anyBoolean());

        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.NO_VIABLE_SUGGESTION);
    }

    @Test
    void statesInabilityExplicitlyWhenTheModelItselfReportsNoViableOptionFIN05() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ, OBEROI));
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenReturn(
            "{\"selectedSupplierRateIds\":[],\"viable\":false,\"reason\":\"No option fits a budget of 1000 INR\"}");

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Something under 1000 INR", new Money(BigDecimal.valueOf(1000), CurrencyCode.INR)));

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        assertThat(((NoViableSuggestion) result).reason()).isEqualTo("No option fits a budget of 1000 INR");
    }

    @Test
    void rejectsAHallucinatedSupplierRateIdNeverPresentInTheLiveCandidateListFIN02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ));
        // The model claims a supplierRateId that was never in the candidate
        // list it was given — a fabrication, must be rejected regardless of
        // "viable": true.
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenReturn(
            "{\"selectedSupplierRateIds\":[\"made-up-rate-id\"],\"viable\":true,\"reason\":null}");

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null));

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        assertThat(((NoViableSuggestion) result).reason()).contains("not present in the live candidate list");
    }

    @Test
    void rejectsASelectionThatExceedsTheStatedBudgetEvenIfTheModelClaimsItFitsFIN02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(OBEROI));
        // OBEROI's real net rate is 15,000 INR — the model claims it fits
        // a 1,000 INR budget; the real candidate data wins, not the model's claim.
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenReturn(
            "{\"selectedSupplierRateIds\":[\"rate-oberoi-1\"],\"viable\":true,\"reason\":null}");

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Something cheap", new Money(BigDecimal.valueOf(1000), CurrencyCode.INR)));

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        assertThat(((NoViableSuggestion) result).reason()).contains("exceeds the stated budget");
    }

    @Test
    void logsTheGroqFailureAndRethrowsRatherThanFabricatingASuggestion() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ));
        GroqClient.GroqAuthenticationException groqFailure = new GroqClient.GroqAuthenticationException(401, null);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenThrow(groqFailure);

        assertThatThrownBy(() -> service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null)))
            .isSameAs(groqFailure);

        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.GROQ_ERROR);
    }

    /**
     * AI-07's own AC, verbatim: "Given 100 AI calls are made in a load
     * test, when the audit log is inspected, then exactly 100 entries
     * exist — zero sampling." Run here with mocked {@code GroqClient}/
     * {@code SupplierSearchApi} (fast, deterministic, no dependency on the
     * real Groq API's rate limits) so all 100 calls genuinely run
     * concurrently against the SAME {@code AiServiceImpl} instance —
     * {@code AiSuggestionAuditLogRecorder.record} is a plain interaction
     * on a Mockito mock, and Mockito's invocation recording is itself
     * thread-safe, so counting exactly 100 calls afterward is a valid
     * proof this method never drops/batches/samples a write under
     * concurrent load. Each thread authenticates itself independently
     * ({@code SecurityContextHolder} is thread-local by default) with its
     * own distinct {@code itineraryId}/{@code consultantId} pair.
     */
    @Test
    void logsExactlyOneAuditEntryPerCallUnderOneHundredConcurrentGenerationsFIN07() throws InterruptedException {
        int callCount = 100;
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ, OBEROI));
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean()))
            .thenReturn("{\"selectedSupplierRateIds\":[\"rate-taj-1\"],\"viable\":true,\"reason\":null}");

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(callCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(callCount);
        List<UUID> itineraryIds = new CopyOnWriteArrayList<>();

        for (int i = 0; i < callCount; i++) {
            executor.submit(() -> {
                UUID consultantId = UUID.randomUUID();
                UUID itineraryId = UUID.randomUUID();
                itineraryIds.add(itineraryId);
                authenticateAs(Role.CONSULTANT, consultantId);
                try {
                    ready.countDown();
                    start.await();
                    service.generateItinerary(new GenerateItineraryCommand(consultantId, itineraryId, "GOA",
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything", null));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    SecurityContextHolder.clearContext();
                    done.countDown();
                }
            });
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).as("all %d calls completed in time", callCount).isTrue();
        executor.shutdown();

        assertThat(itineraryIds).hasSize(callCount).doesNotHaveDuplicates();

        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder, times(callCount)).record(captor.capture());
        Set<UUID> distinctAuditLogIds = captor.getAllValues().stream()
            .map(AiSuggestionAuditLog::getAuditLogId)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(distinctAuditLogIds).as("every audit log row has a distinct id, none overwritten").hasSize(callCount);
    }

    @Test
    void aConsultantCannotGenerateASuggestionForAnotherConsultantFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);

        assertThatThrownBy(() -> service.generateItinerary(new GenerateItineraryCommand(
            otherConsultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null)))
            .isInstanceOf(AccessDeniedException.class);
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
