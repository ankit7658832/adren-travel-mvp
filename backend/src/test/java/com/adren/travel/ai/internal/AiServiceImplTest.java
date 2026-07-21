package com.adren.travel.ai.internal;

import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.ai.AdCreativeSuggestion;
import com.adren.travel.ai.AiAuditLogEntryView;
import com.adren.travel.ai.AiGovernanceSummaryView;
import com.adren.travel.ai.AiItineraryGenerationResult;
import com.adren.travel.ai.AiItinerarySuggestion;
import com.adren.travel.ai.AiPricingRevalidationResult;
import com.adren.travel.ai.AiSuggestedLineItem;
import com.adren.travel.ai.ApproveAiSuggestionCommand;
import com.adren.travel.ai.GenerateAdCreativeCommand;
import com.adren.travel.ai.GenerateItineraryCommand;
import com.adren.travel.ai.NoViableAdCreative;
import com.adren.travel.ai.NoViableSuggestion;
import com.adren.travel.ai.PricingConfirmed;
import com.adren.travel.ai.PricingStale;
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
import java.time.Instant;
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
import static org.mockito.ArgumentMatchers.argThat;
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
    AiSuggestionAuditLogRepository auditLogRepository;

    @Mock
    AiSuggestionAuditLogRecorder auditLogRecorder;

    @Mock
    AiSuggestionApprovalRepository approvalRepository;

    @Mock
    AdCreativeAuditLogRecorder adCreativeAuditLogRecorder;

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
        GroqProperties groqProperties = new GroqProperties("https://api.groq.com/openai/v1", "test-key",
            "llama-3.3-70b-versatile", 15, 2);
        service = new AiServiceImpl(groqClient, supplierSearchApi, auditLogRepository, auditLogRecorder,
            approvalRepository, adCreativeAuditLogRecorder, groqProperties, new ObjectMapper(), events);
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
            "A relaxing beach trip", null, false));

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
            "Anything goes", null, false));

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
            "Something under 1000 INR", new Money(BigDecimal.valueOf(1000), CurrencyCode.INR), false));

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
            "Anything", null, false));

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
            "Something cheap", new Money(BigDecimal.valueOf(1000), CurrencyCode.INR), false));

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        assertThat(((NoViableSuggestion) result).reason()).contains("exceeds the stated budget");
    }

    @Test
    void logsTheGroqFailureAndThrowsAPublicUnavailableExceptionRatherThanFabricatingASuggestion() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ));
        GroqClient.GroqAuthenticationException groqFailure = new GroqClient.GroqAuthenticationException(401, null);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenThrow(groqFailure);

        // AiServiceUnavailableException, not the raw internal GroqClientException
        // — see that class's Javadoc (Stage 4 Step C adversarial finding: the
        // internal type must never cross ai's public boundary, RULES.md §4.1).
        assertThatThrownBy(() -> service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null, false)))
            .isInstanceOf(com.adren.travel.ai.AiServiceUnavailableException.class);

        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.GROQ_ERROR);
        // The real Groq error detail is still fully captured in the audit
        // trail even though the client-facing exception is now generic.
        assertThat(captor.getValue().getAiOutputJson()).isEqualTo(groqFailure.getMessage());
    }

    @Test
    void aTimeoutIsRetriedUpToMaxRetriesWithEachAttemptLoggedDistinctlyThenRethrownAI13() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ));
        GroqClient.GroqTimeoutException timeout = new GroqClient.GroqTimeoutException(15, null);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenThrow(timeout);

        // groqProperties in setUp() is configured with maxRetries=2, so 3
        // total attempts (1 original + 2 retries) before giving up.
        assertThatThrownBy(() -> service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null, false)))
            .isInstanceOf(com.adren.travel.ai.AiServiceUnavailableException.class);

        verify(groqClient, times(3)).chatCompletion(any(), any(), anyBoolean());
        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder, times(3)).record(captor.capture());
        List<Integer> attemptNumbers = captor.getAllValues().stream()
            .map(AiSuggestionAuditLog::getAttemptNumber)
            .toList();
        assertThat(attemptNumbers).containsExactly(1, 2, 3);
        assertThat(captor.getAllValues()).allSatisfy(log ->
            assertThat(log.getDisposition()).isEqualTo(AiSuggestionDisposition.GROQ_ERROR));
        // Every attempt shares one correlationId, distinguishing "3
        // attempts of the same request" from "3 unrelated requests."
        Set<UUID> correlationIds = captor.getAllValues().stream()
            .map(AiSuggestionAuditLog::getCorrelationId)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(correlationIds).hasSize(1);
    }

    @Test
    void aTimeoutThatSucceedsOnRetryReturnsAGroundedSuggestionAI13() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ));
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean()))
            .thenThrow(new GroqClient.GroqTimeoutException(15, null))
            .thenReturn("{\"selectedSupplierRateIds\":[\"rate-taj-1\"],\"viable\":true,\"reason\":null}");

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null, false));

        assertThat(result).isInstanceOf(AiItinerarySuggestion.class);
        verify(groqClient, times(2)).chatCompletion(any(), any(), anyBoolean());
        // One GROQ_ERROR row for the failed attempt, one SUGGESTED row for
        // the successful one — never overwritten into a single row.
        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder, times(2)).record(captor.capture());
        assertThat(captor.getAllValues()).extracting(AiSuggestionAuditLog::getDisposition)
            .containsExactly(AiSuggestionDisposition.GROQ_ERROR, AiSuggestionDisposition.SUGGESTED);
    }

    @Test
    void aRateLimitFailureIsRetriedButAnAuthFailureIsNotAI13() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(supplierSearchApi.searchHotels(anyString(), any(), any())).thenReturn(List.of(TAJ));
        GroqClient.GroqAuthenticationException authFailure = new GroqClient.GroqAuthenticationException(401, null);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean()))
            .thenThrow(new GroqClient.GroqRateLimitException(null))
            .thenThrow(authFailure);

        assertThatThrownBy(() -> service.generateItinerary(new GenerateItineraryCommand(
            consultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null, false)))
            .isInstanceOf(com.adren.travel.ai.AiServiceUnavailableException.class);

        // Rate limit (attempt 1) is retryable, so a 2nd attempt happens;
        // that 2nd attempt hits an auth failure, which is NOT retryable,
        // so it stops there rather than exhausting all 3 configured attempts.
        verify(groqClient, times(2)).chatCompletion(any(), any(), anyBoolean());
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
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Anything", null, false));
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
    void respectsAnExistingHotelSelectionAndNeverCallsGroqOrSearchHotelsAI03() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        AiItineraryGenerationResult result = service.generateItinerary(new GenerateItineraryCommand(
            consultantId, itineraryId, "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null, true));

        assertThat(result).isInstanceOf(NoViableSuggestion.class);
        assertThat(((NoViableSuggestion) result).reason()).contains("already selected");
        verify(supplierSearchApi, org.mockito.Mockito.never()).searchHotels(any(), any(), any());
        verify(groqClient, org.mockito.Mockito.never()).chatCompletion(any(), any(), anyBoolean());

        // Still 100% audit-logged (backend-best-practices §7) — a
        // "Complete with AI" click that respects an existing selection is
        // still an AI-workflow touchpoint the governance trail must show.
        ArgumentCaptor<AiSuggestionAuditLog> captor = ArgumentCaptor.forClass(AiSuggestionAuditLog.class);
        verify(auditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.NO_VIABLE_SUGGESTION);
        assertThat(captor.getValue().getItineraryId()).isEqualTo(itineraryId);
    }

    @Test
    void aConsultantCannotGenerateASuggestionForAnotherConsultantFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);

        assertThatThrownBy(() -> service.generateItinerary(new GenerateItineraryCommand(
            otherConsultantId, UUID.randomUUID(), "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
            "Anything", null, false)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approvingAnUneditedSuggestionRecordsWasEditedFalseFIN08() {
        UUID consultantId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        AiSuggestedLineItem lineItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        String suggestedJson = new ObjectMapper().writeValueAsString(List.of(lineItem));
        AiSuggestionAuditLog originalLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            UUID.randomUUID(), "{}", "[]", "{}", suggestedJson, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(originalLog));
        UUID approvedByUserId = UUID.randomUUID();

        service.approveAiSuggestion(new ApproveAiSuggestionCommand(auditLogId, approvedByUserId, List.of(lineItem)));

        ArgumentCaptor<AiSuggestionApproval> captor = ArgumentCaptor.forClass(AiSuggestionApproval.class);
        verify(approvalRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isWasEdited()).isFalse();
        assertThat(captor.getValue().getAuditLogId()).isEqualTo(auditLogId);
        assertThat(captor.getValue().getApprovedByUserId()).isEqualTo(approvedByUserId);
        // The original row itself is never touched — no update method
        // exists on AiSuggestionAuditLog/its repository to even attempt it.
        assertThat(originalLog.getSuggestedLineItemsJson()).isEqualTo(suggestedJson);
    }

    @Test
    void approvingAnEditedSuggestionRecordsWasEditedTrueAndPreservesTheOriginalFIN08() {
        UUID consultantId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        AiSuggestedLineItem original = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        String originalSuggestedJson = new ObjectMapper().writeValueAsString(List.of(original));
        AiSuggestionAuditLog originalLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            UUID.randomUUID(), "{}", "[]", "{}", originalSuggestedJson, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(originalLog));
        // The Consultant edited the room type before approving.
        AiSuggestedLineItem edited = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Ocean View Suite", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), original.availabilityAsOf());

        service.approveAiSuggestion(new ApproveAiSuggestionCommand(auditLogId, UUID.randomUUID(), List.of(edited)));

        ArgumentCaptor<AiSuggestionApproval> captor = ArgumentCaptor.forClass(AiSuggestionApproval.class);
        verify(approvalRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isWasEdited()).isTrue();
        assertThat(captor.getValue().getEditedFinalVersionJson()).contains("Ocean View Suite");
        // T14: both versions are independently present afterward — the
        // original (unedited) row, untouched, plus this new edited one.
        assertThat(originalLog.getSuggestedLineItemsJson()).contains("Deluxe Room").doesNotContain("Ocean View Suite");
    }

    @Test
    void approvingAnUnknownAuditLogIdThrows() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        UUID unknownAuditLogId = UUID.randomUUID();
        when(auditLogRepository.findById(unknownAuditLogId)).thenReturn(java.util.Optional.empty());
        AiSuggestedLineItem lineItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());

        assertThatThrownBy(() -> service.approveAiSuggestion(
            new ApproveAiSuggestionCommand(unknownAuditLogId, UUID.randomUUID(), List.of(lineItem))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revalidateAiPricingAtBookingReturnsConfirmedWhenTheLivePriceStillMatchesAI09() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        AiSuggestedLineItem approvedItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        String suggestedJson = new ObjectMapper().writeValueAsString(List.of(approvedItem));
        AiSuggestionAuditLog auditLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            itineraryId, "{\"locationCode\":\"GOA\",\"checkIn\":\"2026-08-01\",\"checkOut\":\"2026-08-05\"}",
            "[]", "{}", suggestedJson, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(auditLog));
        when(approvalRepository.findByAuditLogId(auditLogId)).thenReturn(List.of());
        when(supplierSearchApi.searchHotels("GOA", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5)))
            .thenReturn(List.of(TAJ));

        AiPricingRevalidationResult result = service.revalidateAiPricingAtBooking(auditLogId);

        assertThat(result).isInstanceOf(PricingConfirmed.class);
        verify(events).publishEvent(argThat((com.adren.travel.ai.event.AiPricingRevalidatedEvent e) -> !e.stale()));
    }

    @Test
    void revalidateAiPricingAtBookingReturnsStaleWhenTheLivePriceHasChangedAI09() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        AiSuggestedLineItem approvedItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        String suggestedJson = new ObjectMapper().writeValueAsString(List.of(approvedItem));
        AiSuggestionAuditLog auditLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            itineraryId, "{\"locationCode\":\"GOA\",\"checkIn\":\"2026-08-01\",\"checkOut\":\"2026-08-05\"}",
            "[]", "{}", suggestedJson, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(auditLog));
        when(approvalRepository.findByAuditLogId(auditLogId)).thenReturn(List.of());
        // The live price has risen from 5000 to 6500 INR since approval.
        SupplierSearchResult repricedTaj = new SupplierSearchResult(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(6500), CurrencyCode.INR), 4.5);
        when(supplierSearchApi.searchHotels("GOA", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5)))
            .thenReturn(List.of(repricedTaj));

        AiPricingRevalidationResult result = service.revalidateAiPricingAtBooking(auditLogId);

        assertThat(result).isInstanceOf(PricingStale.class);
        assertThat(((PricingStale) result).reason()).contains("5000").contains("6500").contains("please confirm");
        verify(events).publishEvent(argThat((com.adren.travel.ai.event.AiPricingRevalidatedEvent e) -> e.stale()));
    }

    @Test
    void revalidateAiPricingAtBookingReturnsStaleWhenTheApprovedRateIsNoLongerAvailableAI09() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        AiSuggestedLineItem approvedItem = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-taj-1", "Taj Palace",
            "Deluxe Room", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), Instant.now());
        String suggestedJson = new ObjectMapper().writeValueAsString(List.of(approvedItem));
        AiSuggestionAuditLog auditLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            itineraryId, "{\"locationCode\":\"GOA\",\"checkIn\":\"2026-08-01\",\"checkOut\":\"2026-08-05\"}",
            "[]", "{}", suggestedJson, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(auditLog));
        when(approvalRepository.findByAuditLogId(auditLogId)).thenReturn(List.of());
        // OBEROI only — the approved Taj Palace rate is gone entirely.
        when(supplierSearchApi.searchHotels("GOA", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5)))
            .thenReturn(List.of(OBEROI));

        AiPricingRevalidationResult result = service.revalidateAiPricingAtBooking(auditLogId);

        assertThat(result).isInstanceOf(PricingStale.class);
        assertThat(((PricingStale) result).reason()).contains("no longer available");
    }

    @Test
    void revalidateAiPricingAtBookingComparesTheApprovedEditedVersionNotTheOriginalSuggestionAI09() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        // The ORIGINAL suggestion selected a rate that no longer exists —
        // if this were compared, it would be stale.
        AiSuggestedLineItem originallySuggested = new AiSuggestedLineItem(SupplierId.HOTELBEDS, "rate-gone",
            "Ghost Hotel", "Any Room", new Money(BigDecimal.valueOf(9999), CurrencyCode.INR), Instant.now());
        String suggestedJson = new ObjectMapper().writeValueAsString(List.of(originallySuggested));
        // The Consultant EDITED the suggestion before approving, swapping
        // in OBEROI at its exact live price — this is what must be checked.
        AiSuggestedLineItem editedApproval = new AiSuggestedLineItem(SupplierId.STUBA, "rate-oberoi-1",
            "The Oberoi", "Luxury Suite", new Money(BigDecimal.valueOf(15000), CurrencyCode.INR), Instant.now());
        String editedJson = new ObjectMapper().writeValueAsString(List.of(editedApproval));
        AiSuggestionAuditLog auditLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            itineraryId, "{\"locationCode\":\"GOA\",\"checkIn\":\"2026-08-01\",\"checkOut\":\"2026-08-05\"}",
            "[]", "{}", suggestedJson, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(auditLog));
        AiSuggestionApproval approval = new AiSuggestionApproval(UUID.randomUUID(), auditLogId,
            UUID.randomUUID(), editedJson, true);
        when(approvalRepository.findByAuditLogId(auditLogId)).thenReturn(List.of(approval));
        when(supplierSearchApi.searchHotels("GOA", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5)))
            .thenReturn(List.of(OBEROI));

        AiPricingRevalidationResult result = service.revalidateAiPricingAtBooking(auditLogId);

        assertThat(result).isInstanceOf(PricingConfirmed.class);
    }

    @Test
    void revalidateAiPricingAtBookingCannotBeCalledForAnotherConsultantsAuditLogFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        AiSuggestionAuditLog auditLog = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1,
            otherConsultantId, UUID.randomUUID(), "{}", "[]", "{}", null, AiSuggestionDisposition.SUGGESTED);
        when(auditLogRepository.findById(auditLogId)).thenReturn(java.util.Optional.of(auditLog));

        assertThatThrownBy(() -> service.revalidateAiPricingAtBooking(auditLogId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generateAdCreativeReturnsGroundedVariantsWhenEveryVariantReferencesTheRealNameAndPriceAI12() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money price = new Money(BigDecimal.valueOf(25000), CurrencyCode.INR);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenReturn("""
            {"variants": [
                {"headline": "Goa Getaway", "bodyText": "Book Goa Beach Escape now for just 25000.00 INR!"},
                {"headline": "Limited offer", "bodyText": "Goa Beach Escape — only 25000.00 INR, book today."}
            ]}
            """);

        AdCreativeGenerationResult result = service.generateAdCreative(new GenerateAdCreativeCommand(
            consultantId, packageId, "Goa Beach Escape", "A relaxing beach package", price, 2));

        assertThat(result).isInstanceOf(AdCreativeSuggestion.class);
        AdCreativeSuggestion suggestion = (AdCreativeSuggestion) result;
        assertThat(suggestion.variants()).hasSize(2);
        assertThat(suggestion.variants()).allSatisfy(v -> {
            assertThat(v.bodyText()).contains("Goa Beach Escape").contains("25000.00 INR");
        });

        ArgumentCaptor<AdCreativeAuditLog> captor = ArgumentCaptor.forClass(AdCreativeAuditLog.class);
        verify(adCreativeAuditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.SUGGESTED);
        assertThat(captor.getValue().getPackageId()).isEqualTo(packageId);
    }

    @Test
    void generateAdCreativeDropsVariantsThatDoNotReferenceTheRealNameOrPriceAI12() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money price = new Money(BigDecimal.valueOf(25000), CurrencyCode.INR);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenReturn("""
            {"variants": [
                {"headline": "Grounded", "bodyText": "Book Goa Beach Escape now for just 25000.00 INR!"},
                {"headline": "Fabricated", "bodyText": "Book the Bali Adventure now for only 9999.00 INR!"}
            ]}
            """);

        AdCreativeGenerationResult result = service.generateAdCreative(new GenerateAdCreativeCommand(
            consultantId, packageId, "Goa Beach Escape", "A relaxing beach package", price, 2));

        assertThat(result).isInstanceOf(AdCreativeSuggestion.class);
        AdCreativeSuggestion suggestion = (AdCreativeSuggestion) result;
        assertThat(suggestion.variants()).hasSize(1);
        assertThat(suggestion.variants().get(0).headline()).isEqualTo("Grounded");
    }

    @Test
    void generateAdCreativeReturnsNoViableWhenEveryVariantFailsGroundingAI12() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money price = new Money(BigDecimal.valueOf(25000), CurrencyCode.INR);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenReturn(
            "{\"variants\": [{\"headline\": \"Fabricated\", \"bodyText\": \"Book the Bali Adventure for 9999.00 INR!\"}]}");

        AdCreativeGenerationResult result = service.generateAdCreative(new GenerateAdCreativeCommand(
            consultantId, packageId, "Goa Beach Escape", "A relaxing beach package", price, 1));

        assertThat(result).isInstanceOf(NoViableAdCreative.class);
        assertThat(((NoViableAdCreative) result).reason()).contains("real name and exact current price");
        ArgumentCaptor<AdCreativeAuditLog> captor = ArgumentCaptor.forClass(AdCreativeAuditLog.class);
        verify(adCreativeAuditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.NO_VIABLE_SUGGESTION);
    }

    @Test
    void generateAdCreativeLogsTheGroqFailureAndThrowsAPublicUnavailableExceptionRatherThanFabricatingCopyAI12() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money price = new Money(BigDecimal.valueOf(25000), CurrencyCode.INR);
        GroqClient.GroqAuthenticationException groqFailure = new GroqClient.GroqAuthenticationException(401, null);
        when(groqClient.chatCompletion(anyString(), anyString(), anyBoolean())).thenThrow(groqFailure);

        assertThatThrownBy(() -> service.generateAdCreative(new GenerateAdCreativeCommand(
            consultantId, packageId, "Goa Beach Escape", "A relaxing beach package", price, 2)))
            .isInstanceOf(com.adren.travel.ai.AiServiceUnavailableException.class);

        ArgumentCaptor<AdCreativeAuditLog> captor = ArgumentCaptor.forClass(AdCreativeAuditLog.class);
        verify(adCreativeAuditLogRecorder).record(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(AiSuggestionDisposition.GROQ_ERROR);
    }

    @Test
    void generateAdCreativeCannotBeCalledOnAnotherConsultantsBehalfFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        Money price = new Money(BigDecimal.valueOf(25000), CurrencyCode.INR);

        assertThatThrownBy(() -> service.generateAdCreative(new GenerateAdCreativeCommand(
            otherConsultantId, UUID.randomUUID(), "Goa Beach Escape", "A relaxing beach package", price, 1)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findAuditLogFiltersByConsultantWhenOneIsGivenAI11() {
        UUID consultantId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        AiSuggestionAuditLog log = new AiSuggestionAuditLog(auditLogId, UUID.randomUUID(), 1, consultantId,
            itineraryId, "{}", "[]", "{}", null, AiSuggestionDisposition.SUGGESTED);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(auditLogRepository.findByConsultantIdOrderByCreatedAtDesc(consultantId, pageable))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(log)));

        AiAuditLogEntryView view = service.findAuditLog(consultantId, pageable).getContent().get(0);

        assertThat(view.auditLogId()).isEqualTo(auditLogId);
        assertThat(view.consultantId()).isEqualTo(consultantId);
        assertThat(view.itineraryId()).isEqualTo(itineraryId);
        assertThat(view.disposition()).isEqualTo("SUGGESTED");
        verify(auditLogRepository, org.mockito.Mockito.never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    void findAuditLogBrowsesEveryConsultantWhenNoneIsGivenAI11() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable))
            .thenReturn(org.springframework.data.domain.Page.empty());

        service.findAuditLog(null, pageable);

        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(pageable);
        verify(auditLogRepository, org.mockito.Mockito.never()).findByConsultantIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void findAiGovernanceSummarySumsCountsAcrossEveryDispositionHRD11() {
        when(auditLogRepository.countByDisposition(AiSuggestionDisposition.SUGGESTED)).thenReturn(10L);
        when(auditLogRepository.countByDisposition(AiSuggestionDisposition.NO_VIABLE_SUGGESTION)).thenReturn(3L);
        when(auditLogRepository.countByDisposition(AiSuggestionDisposition.GROQ_ERROR)).thenReturn(2L);

        AiGovernanceSummaryView summary = service.findAiGovernanceSummary();

        assertThat(summary.suggestedCount()).isEqualTo(10L);
        assertThat(summary.noViableSuggestionCount()).isEqualTo(3L);
        assertThat(summary.groqErrorCount()).isEqualTo(2L);
        assertThat(summary.totalSuggestions()).isEqualTo(15L);
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
