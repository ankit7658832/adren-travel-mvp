package com.adren.travel.ai.internal;

import com.adren.travel.ai.AdCreativeGenerationResult;
import com.adren.travel.ai.AdCreativeSuggestion;
import com.adren.travel.ai.AdCreativeVariant;
import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.AiAuditLogEntryView;
import com.adren.travel.ai.AiGovernanceSummaryView;
import com.adren.travel.ai.AiServiceUnavailableException;
import com.adren.travel.ai.AiItineraryGenerationResult;
import com.adren.travel.ai.AiItinerarySuggestion;
import com.adren.travel.ai.AiPricingRevalidationResult;
import com.adren.travel.ai.ApproveAiSuggestionCommand;
import com.adren.travel.ai.AiSuggestedLineItem;
import com.adren.travel.ai.GenerateAdCreativeCommand;
import com.adren.travel.ai.GenerateItineraryCommand;
import com.adren.travel.ai.NoViableAdCreative;
import com.adren.travel.ai.NoViableSuggestion;
import com.adren.travel.ai.PricingConfirmed;
import com.adren.travel.ai.PricingStale;
import com.adren.travel.ai.event.AdCreativeGeneratedEvent;
import com.adren.travel.ai.event.AiPricingRevalidatedEvent;
import com.adren.travel.ai.event.AiSuggestionGeneratedEvent;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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

    // AI-12, PRD §14.4 — same belt-and-suspenders shape as SYSTEM_PROMPT
    // above: the prompt instructs grounding, but groundAdCreativeVariants
    // is what actually ENFORCES it, dropping any variant whose bodyText
    // doesn't literally contain the real package name/price regardless of
    // how plausible the copy otherwise reads.
    private static final String AD_CREATIVE_SYSTEM_PROMPT = """
        You are an advertising copywriter for a B2B travel booking platform.
        You will be given a REAL travel package's exact name, description,
        and current sell price. Generate the requested number of ad-creative
        variants (a short headline and a body text) for this package. Every
        variant's bodyText MUST literally include the package's exact name
        and its exact current price as given — never invent, omit, or alter
        either, and never reference any other price.

        Respond with ONLY a JSON object of this exact shape, no other text:
        {"variants": [{"headline": "...", "bodyText": "..."}, ...]}
        """;

    private final GroqClient groqClient;
    private final SupplierSearchApi supplierSearchApi;
    private final AiSuggestionAuditLogRepository auditLogRepository;
    private final AiSuggestionAuditLogRecorder auditLogRecorder;
    private final AiSuggestionApprovalRepository approvalRepository;
    private final AdCreativeAuditLogRecorder adCreativeAuditLogRecorder;
    private final GroqProperties groqProperties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    AiServiceImpl(GroqClient groqClient, SupplierSearchApi supplierSearchApi,
                  AiSuggestionAuditLogRepository auditLogRepository, AiSuggestionAuditLogRecorder auditLogRecorder,
                  AiSuggestionApprovalRepository approvalRepository, AdCreativeAuditLogRecorder adCreativeAuditLogRecorder,
                  GroqProperties groqProperties, ObjectMapper objectMapper, ApplicationEventPublisher events) {
        this.groqClient = groqClient;
        this.supplierSearchApi = supplierSearchApi;
        this.auditLogRepository = auditLogRepository;
        this.auditLogRecorder = auditLogRecorder;
        this.approvalRepository = approvalRepository;
        this.adCreativeAuditLogRecorder = adCreativeAuditLogRecorder;
        this.groqProperties = groqProperties;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @Override
    @Transactional
    public AiItineraryGenerationResult generateItinerary(GenerateItineraryCommand command) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(command.consultantId());
        UUID correlationId = UUID.randomUUID();
        Instant availabilityAsOf = Instant.now();

        String requestInputJson = objectMapper.writeValueAsString(command);

        // AI-03, PRD §9.1 Flow A step 7: "Complete with AI" on a
        // partially-built itinerary must respect what the Consultant
        // already selected — the hotel category isn't a gap, so this never
        // even calls searchHotels/Groq (nothing to ground a replacement
        // against, and proposing one would contradict "respects existing
        // selections"). Still audit-logged: a "Complete with AI" click is
        // an AI-workflow touchpoint the governance trail must show, even
        // when the outcome is "nothing to add."
        if (command.hasExistingHotelSelection()) {
            return recordNoViableSuggestion(correlationId, scopedConsultantId, command.itineraryId(),
                requestInputJson, "[]", null,
                "Hotel already selected for this itinerary — existing selection respected, no replacement proposed");
        }

        List<SupplierSearchResult> candidates =
            supplierSearchApi.searchHotels(command.locationCode(), command.checkIn(), command.checkOut());
        String candidateSnapshotJson = objectMapper.writeValueAsString(candidates);

        // AI-05, PRD §11.3: zero inventory is stated explicitly — never
        // even reaches the model, since there is nothing to ground against.
        if (candidates.isEmpty()) {
            return recordNoViableSuggestion(correlationId, scopedConsultantId, command.itineraryId(),
                requestInputJson, candidateSnapshotJson, null,
                "No inventory available for " + command.locationCode());
        }

        String rawOutput = callGroqWithBoundedRetries(correlationId, scopedConsultantId, command.itineraryId(),
            requestInputJson, candidateSnapshotJson, buildUserPrompt(command, candidates));

        GroundedSelection selection = validateAndGround(rawOutput, candidates, command.budgetLimit());
        if (selection.lineItems().isEmpty()) {
            return recordNoViableSuggestion(correlationId, scopedConsultantId, command.itineraryId(),
                requestInputJson, candidateSnapshotJson, rawOutput, selection.reason());
        }

        List<AiSuggestedLineItem> lineItems = selection.lineItems().stream()
            .map(c -> new AiSuggestedLineItem(c.supplierId(), c.supplierRateId(), c.propertyName(), c.roomType(),
                c.netRate(), availabilityAsOf))
            .toList();

        UUID auditLogId = UUID.randomUUID();
        auditLogRecorder.record(new AiSuggestionAuditLog(auditLogId, correlationId, 1, scopedConsultantId,
            command.itineraryId(), requestInputJson, candidateSnapshotJson, rawOutput,
            objectMapper.writeValueAsString(lineItems), AiSuggestionDisposition.SUGGESTED));
        events.publishEvent(new AiSuggestionGeneratedEvent(auditLogId, command.itineraryId(), scopedConsultantId,
            AiSuggestionDisposition.SUGGESTED.name()));

        return new AiItinerarySuggestion(auditLogId, lineItems);
    }

    @Override
    @Transactional
    public void approveAiSuggestion(ApproveAiSuggestionCommand command) {
        AiSuggestionAuditLog auditLog = auditLogRepository.findById(command.auditLogId())
            .orElseThrow(() -> new IllegalArgumentException("No AI suggestion audit log: " + command.auditLogId()));
        CurrentPrincipal.resolveTenantScope(auditLog.getConsultantId());

        String finalVersionJson = objectMapper.writeValueAsString(command.finalLineItems());
        // AI-08: never trust a caller-supplied "was this edited" flag —
        // compare the actual final version against what was actually
        // suggested, both serialized the same way.
        boolean wasEdited = !finalVersionJson.equals(auditLog.getSuggestedLineItemsJson());

        approvalRepository.saveAndFlush(new AiSuggestionApproval(UUID.randomUUID(), command.auditLogId(),
            command.approvedByUserId(), finalVersionJson, wasEdited));
    }

    /**
     * AI-11, PRD §6/§21.6 — SUPER_ADMIN-only, enforced by {@link AiApi}'s
     * own {@code @PreAuthorize} (never a self-scoped {@code
     * CurrentPrincipal.resolveTenantScope} check the way every other
     * method here uses, since this is the one company-wide view PRD §6
     * grants).
     */
    @Override
    public Page<AiAuditLogEntryView> findAuditLog(UUID consultantId, Pageable pageable) {
        Page<AiSuggestionAuditLog> page = consultantId != null
            ? auditLogRepository.findByConsultantIdOrderByCreatedAtDesc(consultantId, pageable)
            : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(AiServiceImpl::toAuditLogEntryView);
    }

    private static AiAuditLogEntryView toAuditLogEntryView(AiSuggestionAuditLog log) {
        return new AiAuditLogEntryView(log.getAuditLogId(), log.getCorrelationId(), log.getAttemptNumber(),
            log.getConsultantId(), log.getItineraryId(), log.getRequestInputJson(), log.getSourceDataSnapshotJson(),
            log.getAiOutputJson(), log.getDisposition().name(), log.getCreatedAt());
    }

    @Override
    public AiGovernanceSummaryView findAiGovernanceSummary() {
        long suggestedCount = auditLogRepository.countByDisposition(AiSuggestionDisposition.SUGGESTED);
        long noViableSuggestionCount = auditLogRepository.countByDisposition(AiSuggestionDisposition.NO_VIABLE_SUGGESTION);
        long groqErrorCount = auditLogRepository.countByDisposition(AiSuggestionDisposition.GROQ_ERROR);
        return new AiGovernanceSummaryView(
            suggestedCount + noViableSuggestionCount + groqErrorCount, suggestedCount, noViableSuggestionCount, groqErrorCount);
    }

    /**
     * AI-09, PRD §11.3: re-runs the ORIGINAL search this suggestion was
     * grounded against and compares every approved line item's live net
     * rate to what was actually approved — never trusts the price the
     * Consultant approved earlier as still current. {@code
     * approvalRepository.findByAuditLogId} can return more than one row if
     * {@code approveAiSuggestion} was ever called twice for the same audit
     * log (insert-only, so a re-approval is a new row, not an update) — the
     * most recent one by {@code approvedAt} is what's actually being
     * booked. Falls back to the original {@code suggestedLineItemsJson} if
     * no approval row exists at all (defensive; {@code booking} only calls
     * this for itineraries where {@code Itinerary.isAiGenerated()}, which
     * implies a SUGGESTED disposition, but this method makes no assumption
     * about caller discipline).
     */
    @Override
    @Transactional
    public AiPricingRevalidationResult revalidateAiPricingAtBooking(UUID auditLogId) {
        AiSuggestionAuditLog auditLog = auditLogRepository.findById(auditLogId)
            .orElseThrow(() -> new IllegalArgumentException("No AI suggestion audit log: " + auditLogId));
        UUID consultantId = CurrentPrincipal.resolveTenantScope(auditLog.getConsultantId());

        String finalVersionJson = approvalRepository.findByAuditLogId(auditLogId).stream()
            .max(Comparator.comparing(AiSuggestionApproval::getApprovedAt))
            .map(AiSuggestionApproval::getEditedFinalVersionJson)
            .orElse(auditLog.getSuggestedLineItemsJson());
        if (finalVersionJson == null) {
            return new PricingConfirmed();
        }

        List<AiSuggestedLineItem> approvedLineItems =
            objectMapper.readValue(finalVersionJson, new TypeReference<List<AiSuggestedLineItem>>() {
            });
        OriginalSearchParams params =
            objectMapper.readValue(auditLog.getRequestInputJson(), OriginalSearchParams.class);
        List<SupplierSearchResult> liveCandidates =
            supplierSearchApi.searchHotels(params.locationCode(), params.checkIn(), params.checkOut());
        Map<String, SupplierSearchResult> liveByRateId = liveCandidates.stream()
            .collect(java.util.stream.Collectors.toMap(SupplierSearchResult::supplierRateId, c -> c, (a, b) -> a));

        for (AiSuggestedLineItem item : approvedLineItems) {
            SupplierSearchResult live = liveByRateId.get(item.supplierRateId());
            String staleReason = staleReasonFor(item, live);
            if (staleReason != null) {
                events.publishEvent(new AiPricingRevalidatedEvent(
                    auditLogId, auditLog.getItineraryId(), consultantId, true, staleReason));
                return new PricingStale(staleReason);
            }
        }

        events.publishEvent(new AiPricingRevalidatedEvent(
            auditLogId, auditLog.getItineraryId(), consultantId, false, null));
        return new PricingConfirmed();
    }

    private static String staleReasonFor(AiSuggestedLineItem approved, SupplierSearchResult live) {
        if (live == null) {
            return "Approved option " + approved.supplierRateId() + " (" + approved.propertyName()
                + ") is no longer available from the supplier";
        }
        if (live.netRate().amount().compareTo(approved.netRate().amount()) != 0) {
            return "Live price for " + approved.propertyName() + " has changed from "
                + approved.netRate().amount() + " " + approved.netRate().currency() + " to "
                + live.netRate().amount() + " " + live.netRate().currency() + " — please confirm";
        }
        return null;
    }

    /** Just enough of {@link GenerateItineraryCommand}'s original JSON to re-run the same search (extra fields ignored). */
    private record OriginalSearchParams(String locationCode, LocalDate checkIn, LocalDate checkOut) {
    }

    /**
     * AI-12, PRD §14.4: unlike {@link #generateItinerary}, there is no
     * supplier candidate list to select from — the grounding source IS the
     * caller-supplied Package content itself ({@code
     * ads.AdsApi.generateAdCreativeForPackage} resolves it via {@code
     * BookingApi.findPackageById} before calling this). {@link
     * #groundAdCreativeVariants} enforces that every returned variant's
     * {@code bodyText} literally contains the real name/price — a variant
     * that fails this is dropped, never "no viable" for the whole call
     * unless EVERY variant fails.
     */
    @Override
    @Transactional
    public AdCreativeGenerationResult generateAdCreative(GenerateAdCreativeCommand command) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(command.consultantId());
        String requestInputJson = objectMapper.writeValueAsString(command);
        String sourceSnapshotJson = objectMapper.writeValueAsString(
            new PackageContentSnapshot(command.packageName(), command.packageDescription(), command.currentSellPrice()));

        String rawOutput = callGroqForAdCreativeWithBoundedRetries(scopedConsultantId, command.packageId(),
            requestInputJson, sourceSnapshotJson, buildAdCreativeUserPrompt(command));

        List<AdCreativeVariant> grounded = groundAdCreativeVariants(rawOutput, command);
        UUID auditLogId = UUID.randomUUID();
        if (grounded.isEmpty()) {
            adCreativeAuditLogRecorder.record(new AdCreativeAuditLog(auditLogId, scopedConsultantId,
                command.packageId(), requestInputJson, sourceSnapshotJson, rawOutput,
                AiSuggestionDisposition.NO_VIABLE_SUGGESTION));
            events.publishEvent(new AdCreativeGeneratedEvent(auditLogId, command.packageId(), scopedConsultantId,
                AiSuggestionDisposition.NO_VIABLE_SUGGESTION.name()));
            return new NoViableAdCreative(auditLogId,
                "No generated variant referenced the package's real name and exact current price");
        }

        adCreativeAuditLogRecorder.record(new AdCreativeAuditLog(auditLogId, scopedConsultantId, command.packageId(),
            requestInputJson, sourceSnapshotJson, rawOutput, AiSuggestionDisposition.SUGGESTED));
        events.publishEvent(new AdCreativeGeneratedEvent(auditLogId, command.packageId(), scopedConsultantId,
            AiSuggestionDisposition.SUGGESTED.name()));
        return new AdCreativeSuggestion(auditLogId, grounded);
    }

    private record PackageContentSnapshot(String packageName, String packageDescription, Money currentSellPrice) {
    }

    /**
     * Server-side grounding enforcement for ad creative — never trusts the
     * model's copy at face value. A variant whose {@code bodyText} doesn't
     * literally contain the package's real name AND its exact current
     * price (same string {@link #buildAdCreativeUserPrompt} gave the
     * model) is dropped outright, regardless of how plausible the rest of
     * the copy reads.
     */
    private List<AdCreativeVariant> groundAdCreativeVariants(String rawOutput, GenerateAdCreativeCommand command) {
        GroqAdCreativeResponse parsed;
        try {
            parsed = objectMapper.readValue(rawOutput, GroqAdCreativeResponse.class);
        } catch (RuntimeException e) {
            return List.of();
        }
        if (parsed.variants() == null) {
            return List.of();
        }

        String requiredName = command.packageName();
        String requiredPrice = formatPrice(command.currentSellPrice());
        List<AdCreativeVariant> grounded = new ArrayList<>();
        for (GroqAdCreativeResponse.GroqAdCreativeVariantResponse variant : parsed.variants()) {
            if (variant.headline() == null || variant.bodyText() == null) {
                continue;
            }
            if (!variant.bodyText().contains(requiredName) || !variant.bodyText().contains(requiredPrice)) {
                continue;
            }
            grounded.add(new AdCreativeVariant(variant.headline(), variant.bodyText()));
        }
        return grounded;
    }

    private static String formatPrice(Money money) {
        return money.amount() + " " + money.currency();
    }

    private static String buildAdCreativeUserPrompt(GenerateAdCreativeCommand command) {
        return "Package name: " + command.packageName() + "\n"
            + "Description: " + command.packageDescription() + "\n"
            + "Current price: " + formatPrice(command.currentSellPrice()) + "\n"
            + "Generate " + command.variantCount() + " ad-creative variants.";
    }

    /**
     * Same bounded-retry shape as {@link #callGroqWithBoundedRetries} (see
     * that method's Javadoc for the full reasoning) — kept as a separate
     * method rather than a shared/generalized one since it writes a
     * different audit entity ({@link AdCreativeAuditLog}, package-scoped)
     * to a different recorder.
     */
    private String callGroqForAdCreativeWithBoundedRetries(UUID consultantId, UUID packageId,
                                                             String requestInputJson, String sourceSnapshotJson,
                                                             String userPrompt) {
        int maxAttempts = 1 + groqProperties.maxRetries();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return groqClient.chatCompletion(AD_CREATIVE_SYSTEM_PROMPT, userPrompt, true);
            } catch (GroqClient.GroqClientException e) {
                UUID auditLogId = UUID.randomUUID();
                adCreativeAuditLogRecorder.record(new AdCreativeAuditLog(auditLogId, consultantId, packageId,
                    requestInputJson, sourceSnapshotJson, e.getMessage(), AiSuggestionDisposition.GROQ_ERROR));
                events.publishEvent(new AdCreativeGeneratedEvent(auditLogId, packageId, consultantId,
                    AiSuggestionDisposition.GROQ_ERROR.name()));
                boolean retryable = e instanceof GroqClient.GroqTimeoutException
                    || e instanceof GroqClient.GroqRateLimitException;
                if (!retryable || attempt == maxAttempts) {
                    // Never let ai.internal.GroqClient's exception type
                    // cross this module's public boundary (RULES.md §4.1)
                    // — see AiServiceUnavailableException's Javadoc for the
                    // real client-visible-error bug this closed.
                    throw new AiServiceUnavailableException();
                }
            }
        }
        throw new IllegalStateException(
            "callGroqForAdCreativeWithBoundedRetries exited its loop without returning or throwing");
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
            itineraryId, requestInputJson, candidateSnapshotJson, rawOutput, null,
            AiSuggestionDisposition.NO_VIABLE_SUGGESTION));
        events.publishEvent(new AiSuggestionGeneratedEvent(auditLogId, itineraryId, consultantId,
            AiSuggestionDisposition.NO_VIABLE_SUGGESTION.name()));
        return new NoViableSuggestion(auditLogId, reason);
    }

    /**
     * AI-13, PRD §24.3/§9.6: bounds the Groq call to {@code
     * adren.ai.groq.max-retries} additional attempts beyond the first,
     * and ONLY for the failure modes a retry could plausibly fix — a
     * timeout or a rate limit, never an auth failure (retrying an invalid
     * {@code GROQ_API_KEY} cannot succeed, so failing fast on the first
     * attempt is correct there, not a missed-retry bug). Every attempt,
     * successful or not, gets its OWN {@code AiSuggestionAuditLog} row
     * sharing one {@code correlationId} — the audit trail shows exactly
     * how many attempts a generation took and what each one did, not just
     * the final outcome.
     */
    private String callGroqWithBoundedRetries(UUID correlationId, UUID consultantId, UUID itineraryId,
                                                String requestInputJson, String candidateSnapshotJson,
                                                String userPrompt) {
        int maxAttempts = 1 + groqProperties.maxRetries();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return groqClient.chatCompletion(SYSTEM_PROMPT, userPrompt, true);
            } catch (GroqClient.GroqClientException e) {
                recordGroqError(correlationId, attempt, consultantId, itineraryId, requestInputJson,
                    candidateSnapshotJson, e);
                boolean retryable = e instanceof GroqClient.GroqTimeoutException
                    || e instanceof GroqClient.GroqRateLimitException;
                if (!retryable || attempt == maxAttempts) {
                    // Never let ai.internal.GroqClient's exception type
                    // cross this module's public boundary (RULES.md §4.1)
                    // — see AiServiceUnavailableException's Javadoc for the
                    // real client-visible-error bug this closed.
                    throw new AiServiceUnavailableException();
                }
            }
        }
        // Unreachable — the loop always either returns or throws.
        throw new IllegalStateException("callGroqWithBoundedRetries exited its loop without returning or throwing");
    }

    private void recordGroqError(UUID correlationId, int attemptNumber, UUID consultantId, UUID itineraryId,
                                  String requestInputJson, String candidateSnapshotJson,
                                  GroqClient.GroqClientException e) {
        UUID auditLogId = UUID.randomUUID();
        auditLogRecorder.record(new AiSuggestionAuditLog(auditLogId, correlationId, attemptNumber, consultantId,
            itineraryId, requestInputJson, candidateSnapshotJson, e.getMessage(), null,
            AiSuggestionDisposition.GROQ_ERROR));
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
