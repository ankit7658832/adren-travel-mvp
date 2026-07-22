package com.adren.travel.booking;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 2's promised end-of-stage checkpoint: the full walking-skeleton
 * vertical slice — search, itinerary, quotation, package, direct booking +
 * wallet debit, and (separately) the Stripe webhook path — proven over
 * REAL HTTP (a test-minted, HMAC-signed JWT sent as a real
 * {@code Authorization: Bearer} header, a real embedded servlet container
 * on a random port) against REAL local Postgres. Deliberately NOT
 * Testcontainers-based (unlike {@code BookingEndToEndIT}): this environment
 * has no Docker, so this is the "real backend integration test with a
 * test-minted JWT, run against local Postgres" tier the team agreed to use
 * for this checkpoint instead.
 * <p>
 * The JWT is built directly with the same {@code io.jsonwebtoken} library
 * and claim shape {@code security.internal.JwtTokenService} uses (subject
 * = userId, {@code role} claim, HMAC-signed with the app's real configured
 * {@code adren.security.jwt.secret}) rather than calling that service —
 * it's package-private, and this test intentionally lives in {@code
 * booking} (an already-legitimate {@code booking -> security} dependency
 * edge, exercised elsewhere e.g. {@code NotificationTraceIdPropagationTest})
 * so it can listen for the real {@link BookingConfirmedEvent} on the
 * Stripe leg without creating a NEW module edge. An earlier version of
 * this test lived in {@code security.internal} to reuse {@code
 * JwtTokenService} directly — that made {@code security} import {@code
 * booking.event}, which created a genuine {@code security -> booking ->
 * payments -> security} cycle that Spring Modulith's whole-classpath
 * verification (run by every {@code @ApplicationModuleTest} in the suite,
 * not just this file) correctly rejected. This file's real signature
 * verification of a hand-built token is, if anything, a MORE faithful
 * proof of the JWT filter chain than delegating to the internal signer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullVerticalSliceEndToEndIT {

    /**
     * Captures the Stripe leg's {@code BookingConfirmedEvent} the same way
     * {@code StripeWebhookConfirmsBookingEndToEndIT} does — a plain
     * {@code @SpringBootTest} has no {@code Scenario} utility (that's an
     * {@code @ApplicationModuleTest}-only feature), so a small
     * test-registered listener plus Awaitility observes it instead.
     */
    @TestConfiguration
    static class CaptureConfig {
        @Bean
        AtomicReference<BookingConfirmedEvent> capturedBookingConfirmedEvent() {
            return new AtomicReference<>();
        }

        @Bean
        BookingConfirmedEventCaptureListener bookingConfirmedEventCaptureListener(
            AtomicReference<BookingConfirmedEvent> capturedBookingConfirmedEvent) {
            return new BookingConfirmedEventCaptureListener(capturedBookingConfirmedEvent);
        }
    }

    static class BookingConfirmedEventCaptureListener {
        private final AtomicReference<BookingConfirmedEvent> capturedEvent;

        BookingConfirmedEventCaptureListener(AtomicReference<BookingConfirmedEvent> capturedEvent) {
            this.capturedEvent = capturedEvent;
        }

        @ApplicationModuleListener
        void on(BookingConfirmedEvent event) {
            capturedEvent.set(event);
        }
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP_TYPE =
        new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Value("${adren.security.jwt.secret}")
    private String jwtSecret;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AtomicReference<BookingConfirmedEvent> capturedBookingConfirmedEvent;

    private WebClient webClient;
    private String superAdminToken;

    private final ListAppender<ILoggingEvent> emailAppender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.StubEmailClient")).detachAppender(emailAppender);
    }

    @Test
    void theDirectBookingPathSearchesQuotesPackagesBooksDebitsTheWalletAndNotifiesEndToEnd() {
        startCapturingEmailLog();
        String token = mintSuperAdminToken();
        // BOK-11 (Stage 3 Batch 2) added a real whitelabelApi.findConsultantMarket
        // lookup to publishPackage's ATOL gate — a random, never-onboarded
        // consultantId (Stage 2's original approach) now 400s there. A real
        // onboarded Consultant is required from this point on; this also
        // exercises the actual onboarding endpoint over HTTP as part of the
        // slice, rather than the walking-skeleton's usual "insert directly."
        UUID consultantId = onboardConsultant("Goa Getaways", "INDIA", token);

        // 1. Search — real HTTP call into GeocodeAndSearchService, now
        // fanning out across Hotelbeds+STUBA+TBO in parallel (Stage 3
        // Batch 1) rather than Hotelbeds alone. TBO's stub (4500 INR net)
        // undercuts STUBA's (4800) and Hotelbeds' (5000), so under
        // DefaultSelectionService's deterministic "lowest net rate wins"
        // tie-break (PRD S9.2/S22.2 step 3) TBO is the correct winner now
        // — this replaces Stage 2's stale hardcoded HOTELBEDS assertion,
        // which this very re-run caught as failing once Batch 1 landed.
        Map<String, Object> searchResponse = postJson("/api/v1/search",
            Map.of("locationQueries", List.of("Goa")), token);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> locations = (List<Map<String, Object>>) searchResponse.get("locations");
        assertThat(locations).isNotEmpty();
        Map<String, Object> location = locations.get(0);
        String supplierId = (String) location.get("autoSelectedSupplierId");
        String supplierRateId = (String) location.get("autoSelectedSupplierRateId");
        assertThat(supplierId).isEqualTo("TBO");
        assertThat(supplierRateId).isNotBlank();

        // 2. Itinerary — no HTTP endpoint creates one yet (only the AI
        // Itinerary Governance module, not built in this slice, would call
        // this); every existing test in this codebase inserts one directly,
        // this checkpoint follows the same established convention.
        UUID itineraryId = insertDraftItinerary(consultantId);

        // 2a. Multi-supplier fan-out, proven directly rather than inferred
        // from step 1's single winner: the Itinerary Builder's alternate
        // panel (BOK-20's dedup surface) returns real candidates from ALL
        // THREE suppliers for the same search, not just Hotelbeds.
        List<Map<String, Object>> alternates = webClient().get()
            .uri("/api/v1/itineraries/" + itineraryId + "/alternates?location=Goa")
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .bodyToMono(LIST_OF_MAP_TYPE)
            .block();
        assertThat(alternates).extracting(a -> a.get("supplierId"))
            .containsExactlyInAnyOrder("HOTELBEDS", "STUBA", "TBO");

        // 3. A markup rule must exist before a line item can be priced
        // through FIN-05's pipeline.
        putJson("/api/v1/consultants/" + consultantId + "/markup-rules",
            Map.of("category", "HOTEL", "markupType", "PERCENTAGE", "percentageValue", 15), token);

        // 4. Add a Hotel line item using the search step's real output.
        Map<String, Object> lineItemBody = Map.ofEntries(
            Map.entry("supplierId", supplierId),
            Map.entry("supplierRateId", supplierRateId),
            Map.entry("propertyName", "Taj Exotica"),
            Map.entry("roomType", "Deluxe Room"),
            Map.entry("mealPlan", "BB"),
            Map.entry("cancellationDeadline", Instant.now().plusSeconds(3600).toString()),
            Map.entry("netRate", 5000),
            Map.entry("netRateCurrency", "INR"),
            Map.entry("sellCurrency", "INR"),
            Map.entry("fxRate", 1),
            Map.entry("bufferPercent", 3),
            Map.entry("commissionPercent", 0));
        Map<String, Object> lineItemResponse = postJson(
            "/api/v1/itineraries/" + itineraryId + "/line-items/hotel", lineItemBody, token);
        assertThat(lineItemResponse.get("lineItemId")).isNotNull();

        // 5. Quotation.
        String quotationIdJson = postForRawBody("/api/v1/itineraries/" + itineraryId + "/quotation", null, token);
        UUID quotationId = UUID.fromString(stripQuotes(quotationIdJson));
        assertThat(quotationId).isNotNull();

        // 6. Package — a reusable Package converted from the Quotation.
        Map<String, Object> packageBody = Map.of(
            "name", "Goa Getaway",
            "description", "3-night Goa package",
            "validityStart", Instant.now().toString().substring(0, 10),
            "validityEnd", Instant.now().plusSeconds(90L * 24 * 3600).toString().substring(0, 10),
            "markupPrice", 500,
            "maxPax", 2);
        Map<String, Object> packageResponse = postJson("/api/v1/quotations/" + quotationId + "/package", packageBody, token);
        UUID packageId = UUID.fromString((String) packageResponse.get("packageId"));

        // 7. Publish the Package.
        postForRawBody("/api/v1/packages/" + packageId + "/publish", Map.of("promoteViaAds", false), token);

        // 7b. HRD-15 — the Direct Booking & Payment screen's price-breakdown
        // step reads a single published Package by id; this REST exposure
        // of the already-existing BookingApi.findPackageById (AI-12) never
        // had a caller until HRD-15's frontend.
        Map<String, Object> fetchedPackage = getJson("/api/v1/packages/" + packageId, token);
        assertThat(fetchedPackage.get("name")).isEqualTo("Goa Getaway");
        assertThat(new BigDecimal(fetchedPackage.get("markupPrice").toString())).isEqualByComparingTo("500");

        // 8. Wallet baseline — availableBalance starts at 0 (insertDraftItinerary
        // pre-seeds a 100,000 INR credit_limit so step 9's 11,500 INR
        // confirmation clears FIN-08's credit-limit check).
        Map<String, Object> walletBefore = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletBefore.get("availableBalance").toString())).isEqualByComparingTo("0");

        // 9. Direct (non-Stripe) booking confirmation — places then
        // immediately resolves a wallet hold as a debit (FIN-07), after
        // clearing the FIN-08 credit-limit check.
        BigDecimal totalSellPrice = BigDecimal.valueOf(11_500);
        Map<String, Object> bookingResponse = postJson("/api/v1/bookings",
            Map.of("quotationOrPackageId", packageId.toString(), "totalSellPrice", totalSellPrice, "currency", "INR"),
            token);
        UUID bookingId = UUID.fromString((String) bookingResponse.get("bookingId"));
        assertThat(bookingId).isNotNull();

        // 10. Wallet debited by exactly totalSellPrice.
        Map<String, Object> walletAfter = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletAfter.get("availableBalance").toString()))
            .isEqualByComparingTo(totalSellPrice.negate());
        assertThat(new BigDecimal(walletAfter.get("pendingHolds").toString())).isEqualByComparingTo("0");

        // 11. A Voucher was persisted in the SAME transaction as
        // confirmation (BOK-15) — no public query API exists yet, so this
        // reaches into the table directly, same as every other
        // not-yet-queryable assertion elsewhere in this codebase's tests.
        Map<String, Object> voucherRow = jdbcTemplate.queryForMap(
            "SELECT pdf_reference, atol_certificate_reference FROM voucher WHERE booking_id = ?", bookingId);
        assertThat(voucherRow.get("pdf_reference")).asString().contains(bookingId.toString());
        assertThat(voucherRow.get("atol_certificate_reference")).isNull();

        // 12. Notification dispatch (HRD-01) — email always fires; this
        // asserts against the real StubEmailClient's log line rather than a
        // mock, proving the whole @ApplicationModuleListener chain actually
        // ran for this specific booking.
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(emailAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains(bookingId.toString())))
                .isTrue());

        // 13. FIN-16's cancellation workflow, end to end over real HTTP —
        // cancelled well before the deadline, so no penalty applies and the
        // refund is processed in this same call (no separate approval step).
        Map<String, Object> cancellationBody = Map.of(
            "sellPrice", totalSellPrice,
            "currency", "INR",
            "cancellationDeadline", Instant.now().plusSeconds(3600).toString(),
            "cancelledAt", Instant.now().toString(),
            "postDeadlinePenaltyPercent", 30,
            "originalSupplierCurrency", "INR",
            "originalFxRate", 1,
            "originalFxSnapshotAt", Instant.now().minusSeconds(60).toString());
        Map<String, Object> cancellationResponse = postJson(
            "/api/v1/bookings/" + bookingId + "/cancellation-requests", cancellationBody, token);
        assertThat(cancellationResponse.get("status")).isEqualTo("REFUNDED");

        // 14. The refund actually credited the wallet back to zero — proven
        // against the real ledger, not just the cancellation response.
        Map<String, Object> walletAfterCancellation = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletAfterCancellation.get("availableBalance").toString()))
            .isEqualByComparingTo("0");
        String refundLedgerType = jdbcTemplate.queryForObject(
            "SELECT type FROM wallet_ledger_entry WHERE related_booking_id = ? AND type = 'REFUND'",
            String.class, bookingId);
        assertThat(refundLedgerType).isEqualTo("REFUND");

        // 15. The booking itself moved to CANCELLED.
        String bookingStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
        assertThat(bookingStatus).isEqualTo("CANCELLED");
    }

    @Test
    void confirmingABookingThatWouldBreachTheCreditLimitFailsWithAnActionableMessageEndToEnd() {
        String token = mintSuperAdminToken();
        UUID consultantId = onboardConsultant("Underfunded Travel Co", "INDIA", token);
        // A DRAFT itinerary with a zero credit_limit — deliberately not
        // insertDraftItinerary's usual 100,000 INR seed — so any
        // confirmation attempt has nothing to draw against (FIN-08).
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'DRAFT', false, now(), now())",
            itineraryId, consultantId);
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 0, 0, 'INR', now())",
            consultantId);
        putJson("/api/v1/consultants/" + consultantId + "/markup-rules",
            Map.of("category", "HOTEL", "markupType", "PERCENTAGE", "percentageValue", 15), token);
        Map<String, Object> lineItemBody = Map.ofEntries(
            Map.entry("supplierId", "HOTELBEDS"),
            Map.entry("supplierRateId", "rate-1"),
            Map.entry("propertyName", "Taj Exotica"),
            Map.entry("roomType", "Deluxe Room"),
            Map.entry("mealPlan", "BB"),
            Map.entry("cancellationDeadline", Instant.now().plusSeconds(3600).toString()),
            Map.entry("netRate", 5000),
            Map.entry("netRateCurrency", "INR"),
            Map.entry("sellCurrency", "INR"),
            Map.entry("fxRate", 1),
            Map.entry("bufferPercent", 3),
            Map.entry("commissionPercent", 0));
        postJson("/api/v1/itineraries/" + itineraryId + "/line-items/hotel", lineItemBody, token);
        String quotationIdJson = postForRawBody("/api/v1/itineraries/" + itineraryId + "/quotation", null, token);
        UUID quotationId = UUID.fromString(stripQuotes(quotationIdJson));

        // FIN-08's AC: rejected with an actionable message (a 409 Conflict
        // Problem Detail, not a generic 500) BEFORE any wallet/booking
        // state changes — the credit-limit check runs ahead of the debit.
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.reactive.function.client.WebClientResponseException.class,
            () -> postJson("/api/v1/bookings",
                Map.of("quotationOrPackageId", quotationId.toString(), "totalSellPrice", BigDecimal.valueOf(11_500),
                    "currency", "INR"),
                token));
        assertThat(thrown.getStatusCode().value()).isEqualTo(409);
        assertThat(thrown.getResponseBodyAsString()).contains("credit-limit-exceeded");

        // No booking was ever created, and the wallet is untouched.
        Long bookingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM booking WHERE consultant_id = ?", Long.class, consultantId);
        assertThat(bookingCount).isZero();
        Map<String, Object> walletAfter = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletAfter.get("availableBalance").toString())).isEqualByComparingTo("0");
        assertThat(new BigDecimal(walletAfter.get("pendingHolds").toString())).isEqualByComparingTo("0");
    }

    @Test
    void theStripeWebhookPathConfirmsABookingWithoutTouchingTheWalletEndToEnd() {
        startCapturingEmailLog();
        String token = mintSuperAdminToken();
        UUID consultantId = UUID.randomUUID();
        UUID bookingReferenceId = UUID.randomUUID();

        // Wallet baseline — must stay untouched by this path (the customer
        // paid Stripe directly, not the wallet/credit line).
        Map<String, Object> walletBefore = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletBefore.get("availableBalance").toString())).isEqualByComparingTo("0");

        // 1. Create a Stripe PaymentIntent (FIN-11) — stubbed StripeClient,
        // no real Stripe network call.
        Map<String, Object> intentResponse = postJson("/api/v1/payments/payment-intents",
            Map.of("bookingReferenceId", bookingReferenceId.toString(), "consultantId", consultantId.toString(),
                "amount", 8000, "currency", "INR"),
            token);
        String paymentIntentId = (String) intentResponse.get("paymentIntentId");
        assertThat(paymentIntentId).isNotBlank();

        // 2. The Stripe webhook reports success — this is what
        // StripePaymentConfirmationListener (booking module) reacts to by
        // calling confirmBookingFromPaymentWebhook, never touching the
        // wallet.
        webClient().post().uri("/api/v1/payments/webhooks/stripe")
            .headers(h -> h.setBearerAuth(token))
            .bodyValue(Map.of("type", "payment_intent.succeeded", "paymentIntentId", paymentIntentId))
            .retrieve()
            .toBodilessEntity()
            .block();

        Awaitility.await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(capturedBookingConfirmedEvent.get()).isNotNull());
        UUID bookingId = capturedBookingConfirmedEvent.get().bookingId();
        assertThat(capturedBookingConfirmedEvent.get().consultantId()).isEqualTo(consultantId);

        // 3. Voucher persisted for this Stripe-confirmed booking too.
        Map<String, Object> voucherRow = jdbcTemplate.queryForMap(
            "SELECT pdf_reference FROM voucher WHERE booking_id = ?", bookingId);
        assertThat(voucherRow.get("pdf_reference")).asString().contains(bookingId.toString());

        // 4. Wallet is untouched — FIN-07's Stripe-path contract.
        Map<String, Object> walletAfter = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletAfter.get("availableBalance").toString())).isEqualByComparingTo("0");
        assertThat(new BigDecimal(walletAfter.get("pendingHolds").toString())).isEqualByComparingTo("0");

        // 5. Notification still dispatches on this path too.
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(emailAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains(bookingId.toString())))
                .isTrue());
    }

    private void startCapturingEmailLog() {
        emailAppender.start();
        ((Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.StubEmailClient")).addAppender(emailAppender);
    }

    private String mintSuperAdminToken() {
        if (superAdminToken == null) {
            SecretKey signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            superAdminToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "SUPER_ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
        }
        return superAdminToken;
    }

    /**
     * PRD §13.1 — real Consultant onboarding over HTTP, needed since BOK-11's
     * ATOL gate resolves a real home market. INDIA's required KYC fields
     * (gstRegistration, businessPan, bankDetails per MarketKycRuleProvider)
     * must all be present or onboarding itself 400s.
     */
    private UUID onboardConsultant(String businessName, String homeMarket, String token) {
        Map<String, Object> response = postJson("/api/v1/consultants",
            Map.of("businessName", businessName, "homeMarket", homeMarket, "kycFields",
                Map.of("gstRegistration", "GST123", "businessPan", "PAN123", "bankDetails", "IFSC0001/12345"),
                "email", "owner-" + UUID.randomUUID() + "@example.com",
                "initialPassword", "InitialPassword1!"),
            token);
        return UUID.fromString((String) response.get("consultantId"));
    }

    // BOK-13: confirmBooking/addHotelLineItem all resolve/require a real
    // consultantId already present on the itinerary row.
    private UUID insertDraftItinerary(UUID consultantId) {
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'DRAFT', false, now(), now())",
            itineraryId, consultantId);
        // FIN-08: the direct booking path now enforces the credit limit —
        // seed enough credit for step 9's 11,500 INR confirmation below.
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'INR', now())",
            consultantId);
        return itineraryId;
    }

    private WebClient webClient() {
        if (webClient == null) {
            webClient = WebClient.builder().baseUrl("http://localhost:" + port).build();
        }
        return webClient;
    }

    private Map<String, Object> postJson(String path, Object body, String token) {
        return webClient().post().uri(path)
            .headers(h -> h.setBearerAuth(token))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(MAP_TYPE)
            .block();
    }

    /** For endpoints returning a bare JSON scalar (e.g. a quoted UUID string) rather than an object. */
    private String postForRawBody(String path, Object body, String token) {
        var spec = webClient().post().uri(path).headers(h -> h.setBearerAuth(token));
        return (body != null ? spec.bodyValue(body) : spec)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    private void putJson(String path, Object body, String token) {
        webClient().put().uri(path)
            .headers(h -> h.setBearerAuth(token))
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    private Map<String, Object> getJson(String path, String token) {
        return webClient().get().uri(path)
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .bodyToMono(MAP_TYPE)
            .block();
    }

    private static String stripQuotes(String rawJson) {
        return rawJson.startsWith("\"") && rawJson.endsWith("\"")
            ? rawJson.substring(1, rawJson.length() - 1)
            : rawJson;
    }
}
