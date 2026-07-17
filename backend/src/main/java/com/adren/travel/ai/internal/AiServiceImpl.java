package com.adren.travel.ai.internal;

import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.AiItineraryGenerationResult;
import com.adren.travel.ai.AiItinerarySuggestion;
import com.adren.travel.ai.AiSuggestedLineItem;
import com.adren.travel.ai.GenerateItineraryCommand;
import com.adren.travel.ai.NoViableSuggestion;
import com.adren.travel.ai.event.AiSuggestionGeneratedEvent;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal implementation of {@link AiApi} (PRD §11, AI-02). Not visible
 * outside this module.
 * <p>
 * <b>Grounding enforcement (PRD §11.2 principle 1) is programmatic, not
 * just a prompt instruction.</b> The model is given ONLY the real, live
 * {@link SupplierSearchResult}s {@link SupplierSearchApi#searchHotels}
 * returned and is asked to respond with nothing more than which {@code
 * supplierRateId}s (from that exact list) it's selecting — see {@link
 * GroqSuggestionResponse}'s Javadoc for why the contract is this narrow.
 * {@link #validateAndGround} then checks every returned id against the
 * SAME candidate list the prompt was built from: an id the model invents
 * that isn't in that list, or a selection whose real net rate exceeds the
 * caller's budget, is rejected as a grounding violation regardless of what
 * the model claims — the system never trusts the model's own self-report
 * of viability/pricing over the actual supplier data. This is what makes
 * "AI should not hallucinate" an enforced invariant instead of a hope.
 * <p>
 * <b>The audit-log write is a transactional gate (AI-07), not a
 * fire-and-forget side channel.</b> {@link AiSuggestionAuditLogRecorder}
 * runs the actual insert in its OWN {@code REQUIRES_NEW} transaction (see
 * that class's Javadoc for why a same-transaction {@code saveAndFlush}
 * isn't enough) — but the CALL to it is still synchronous and unguarded
 * here: if the write fails, the exception propagates and the whole method
 * throws, so the caller never receives an {@link AiItinerarySuggestion}
 * that was never actually logged (backend-best-practices §7). The
 * {@code REQUIRES_NEW} boundary additionally means an audit row for a
 * FAILED generation attempt (Groq error) survives even though {@code
 * generateItinerary} itself throws afterward — see this story's own
 * commit message for the real bug this closed (a Groq-failure audit row
 * that was being silently rolled back with the rest of the transaction).
 */
@Service
class AiServiceImpl implements AiApi {

    // PRD §11.2 — the whole system prompt exists to make grounding
    // structural: the model is told explicitly it may ONLY pick from the
    // supplied candidates and must say so plainly when none qualify,
    // matching the AC in AI-05's own story text almost verbatim. This is
    // belt (prompt instruction) AND suspenders (server-side validation in
    // validateAndGround) — never suspenders alone, since a prompt
    // instruction is not a guarantee.
    private static final String SYSTEM_PROMPT = """
        You are a travel itinerary assistant for a B2B travel booking platform.
        You will be given a numbered list of REAL, currently bookable hotel
        options and a traveler's request. You may ONLY select from the exact
        options given to you — never invent a hotel, price, or supplierRateId
        that is not in the list. If no option in the list satisfies the
        traveler's stated budget or preferences, you MUST say so explicitly
        rather than selecting the closest option anyway.

        Respond with ONLY a JSON object of this exact shape, no other text:
        {"selectedSupplierRateIds": ["<supplierRateId from the list>", ...], "viable": true|false, "reason": "<required if viable is false, else null>"}
        """;

    private final GroqClient groqClient;
    private final SupplierSearchApi supplierSearchApi;
    private final AiSuggestionAuditLogRecorder auditLogRecorder;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    AiServiceImpl(GroqClient groqClient, SupplierSearchApi supplierSearchApi,
                  AiSuggestionAuditLogRecorder auditLogRecorder, ObjectMapper objectMapper,
                  ApplicationEventPublisher events) {
        this.groqClient = groqClient;
        this.supplierSearchApi = supplierSearchApi;
        this.auditLogRecorder = auditLogRecorder;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @Override
    @Transactional
    public AiItineraryGenerationResult generateItinerary(GenerateItineraryCommand command) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(command.consultantId());
        UUID correlationId = UUID.randomUUID();
        Instant availabilityAsOf = Instant.now();

        List<SupplierSearchResult> candidates =
            supplierSearchApi.searchHotels(command.locationCode(), command.checkIn(), command.checkOut());
        String candidateSnapshotJson = objectMapper.writeValueAsString(candidates);
        String requestInputJson = objectMapper.writeValueAsString(command);

        // AI-05, PRD §11.3: zero inventory is stated explicitly — never
        // even reaches the model, since there is nothing to ground against.
        if (candidates.isEmpty()) {
            return recordNoViableSuggestion(correlationId, scopedConsultantId, command.itineraryId(),
                requestInputJson, candidateSnapshotJson, null,
                "No inventory available for " + command.locationCode());
        }

        String rawOutput;
        try {
            rawOutput = groqClient.chatCompletion(SYSTEM_PROMPT, buildUserPrompt(command, candidates), true);
        } catch (GroqClient.GroqClientException e) {
            recordGroqError(correlationId, scopedConsultantId, command.itineraryId(),
                requestInputJson, candidateSnapshotJson, e);
            throw e;
        }

        GroundedSelection selection = validateAndGround(rawOutput, candidates, command.budgetLimit());
        if (selection.lineItems().isEmpty()) {
            return recordNoViableSuggestion(correlationId, scopedConsultantId, command.itineraryId(),
                requestInputJson, candidateSnapshotJson, rawOutput, selection.reason());
        }

        UUID auditLogId = UUID.randomUUID();
        auditLogRecorder.record(new AiSuggestionAuditLog(auditLogId, correlationId, 1, scopedConsultantId,
            command.itineraryId(), requestInputJson, candidateSnapshotJson, rawOutput,
            AiSuggestionDisposition.SUGGESTED));
        events.publishEvent(new AiSuggestionGeneratedEvent(auditLogId, command.itineraryId(), scopedConsultantId,
            AiSuggestionDisposition.SUGGESTED.name()));

        List<AiSuggestedLineItem> lineItems = selection.lineItems().stream()
            .map(c -> new AiSuggestedLineItem(c.supplierId(), c.supplierRateId(), c.propertyName(), c.roomType(),
                c.netRate(), availabilityAsOf))
            .toList();
        return new AiItinerarySuggestion(auditLogId, lineItems);
    }

    /**
     * Server-side grounding enforcement — see class Javadoc. Never trusts
     * the model's {@code viable}/{@code selectedSupplierRateIds} claims
     * without cross-checking them against {@code candidates}, the same
     * list the prompt was built from.
     */
    private GroundedSelection validateAndGround(String rawOutput, List<SupplierSearchResult> candidates,
                                                 Money budgetLimit) {
        GroqSuggestionResponse parsed;
        try {
            parsed = objectMapper.readValue(rawOutput, GroqSuggestionResponse.class);
        } catch (RuntimeException e) {
            // A response that isn't even valid JSON in the requested shape
            // is itself a grounding failure, not a viable suggestion.
            return new GroundedSelection(List.of(), "AI response was not in the expected format");
        }

        if (!parsed.viable() || parsed.selectedSupplierRateIds() == null || parsed.selectedSupplierRateIds().isEmpty()) {
            String reason = parsed.reason() != null ? parsed.reason() : "No viable option found for the request";
            return new GroundedSelection(List.of(), reason);
        }

        Map<String, SupplierSearchResult> byRateId = candidates.stream()
            .collect(java.util.stream.Collectors.toMap(SupplierSearchResult::supplierRateId, c -> c, (a, b) -> a));

        List<SupplierSearchResult> grounded = new java.util.ArrayList<>();
        for (String rateId : parsed.selectedSupplierRateIds()) {
            SupplierSearchResult candidate = byRateId.get(rateId);
            if (candidate == null) {
                // The model selected something that was never in the
                // candidate list it was given — a hallucination, rejected
                // outright regardless of what else it claimed.
                return new GroundedSelection(List.of(),
                    "AI selected a supplierRateId not present in the live candidate list — rejected as ungrounded");
            }
            grounded.add(candidate);
        }

        if (budgetLimit != null) {
            BigDecimal total = grounded.stream()
                .map(c -> c.netRate().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(budgetLimit.amount()) > 0) {
                // Never trust the model's own budget compliance claim —
                // the real candidate net rates are compared directly.
                return new GroundedSelection(List.of(),
                    "AI's selection (" + total + " " + budgetLimit.currency()
                        + ") exceeds the stated budget of " + budgetLimit.amount() + " " + budgetLimit.currency());
            }
        }

        return new GroundedSelection(grounded, null);
    }

    private record GroundedSelection(List<SupplierSearchResult> lineItems, String reason) {
    }

    private NoViableSuggestion recordNoViableSuggestion(UUID correlationId, UUID consultantId, UUID itineraryId,
                                                          String requestInputJson, String candidateSnapshotJson,
                                                          String rawOutput, String reason) {
        UUID auditLogId = UUID.randomUUID();
        auditLogRecorder.record(new AiSuggestionAuditLog(auditLogId, correlationId, 1, consultantId,
            itineraryId, requestInputJson, candidateSnapshotJson, rawOutput,
            AiSuggestionDisposition.NO_VIABLE_SUGGESTION));
        events.publishEvent(new AiSuggestionGeneratedEvent(auditLogId, itineraryId, consultantId,
            AiSuggestionDisposition.NO_VIABLE_SUGGESTION.name()));
        return new NoViableSuggestion(auditLogId, reason);
    }

    private void recordGroqError(UUID correlationId, UUID consultantId, UUID itineraryId, String requestInputJson,
                                  String candidateSnapshotJson, GroqClient.GroqClientException e) {
        UUID auditLogId = UUID.randomUUID();
        auditLogRecorder.record(new AiSuggestionAuditLog(auditLogId, correlationId, 1, consultantId,
            itineraryId, requestInputJson, candidateSnapshotJson, e.getMessage(), AiSuggestionDisposition.GROQ_ERROR));
        events.publishEvent(new AiSuggestionGeneratedEvent(auditLogId, itineraryId, consultantId,
            AiSuggestionDisposition.GROQ_ERROR.name()));
    }

    private static String buildUserPrompt(GenerateItineraryCommand command, List<SupplierSearchResult> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Traveler request: ").append(command.naturalLanguageRequest()).append('\n');
        if (command.budgetLimit() != null) {
            sb.append("Budget: ").append(command.budgetLimit().amount())
                .append(' ').append(command.budgetLimit().currency()).append('\n');
        }
        sb.append("Available options:\n");
        for (int i = 0; i < candidates.size(); i++) {
            SupplierSearchResult c = candidates.get(i);
            sb.append(i + 1).append(". supplierRateId=").append(c.supplierRateId())
                .append(", property=").append(c.propertyName())
                .append(", roomType=").append(c.roomType())
                .append(", netRate=").append(c.netRate().amount()).append(' ').append(c.netRate().currency())
                .append(", supplier=").append(c.supplierId())
                .append('\n');
        }
        return sb.toString();
    }
}
