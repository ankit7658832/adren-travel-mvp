package com.adren.travel.booking;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock-complete Definition of Done validation (doc/phases.md S5) — extends
 * {@link FullVerticalSliceEndToEndIT}'s existing product-type-1 (Hotel,
 * INDIA) coverage of the full search->itinerary->quotation->package->
 * publish->booking->payment->voucher->notification->wallet chain with the
 * two things it doesn't cover: a second product type (a UK dynamic
 * flight+hotel combo, exercising BOK-11's real ATOL gate) all the way
 * through to booking/voucher/wallet, and a white-label-themed path
 * (real branding update+fetch for a themed Consultant).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MockCompleteDoDValidationIT {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Value("${adren.security.jwt.secret}")
    private String jwtSecret;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private WebClient webClient;
    private String superAdminToken;

    private final ListAppender<ILoggingEvent> emailAppender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.StubEmailClient")).detachAppender(emailAppender);
    }

    /**
     * Product type 2: a UK dynamic Flight+Hotel combo Package, walked
     * through the SAME real-HTTP chain FullVerticalSliceEndToEndIT proves
     * for Hotel-only, PLUS BOK-11's ATOL disclosure gate specifically.
     * <p>
     * Confirms (not assumes) a genuine, previously-undiscovered gap:
     * {@code VoucherService.generateFor} always persists a {@code null}
     * {@code atolCertificateReference}, regardless of whether the booking
     * is a UK dynamic combo — its own comment claims "no Flight line item
     * type exists yet to ever produce" one, which is stale ({@code
     * FlightLineItem} has existed since BOK-04). Reported, not silently
     * fixed here — a real ATOL-compliance gap is a product decision, not
     * a test-infra one.
     */
    @Test
    void aUkDynamicFlightHotelComboBookingCompletesEndToEndButTheVoucherNeverGetsARealAtolCertificateReference() {
        startCapturingEmailLog();
        String token = mintSuperAdminToken();
        UUID consultantId = onboardUkConsultant("London Luxury Escapes", token);

        UUID itineraryId = insertDraftItinerary(consultantId);
        putJson("/api/v1/consultants/" + consultantId + "/markup-rules",
            Map.of("category", "HOTEL", "markupType", "PERCENTAGE", "percentageValue", 15), token);
        putJson("/api/v1/consultants/" + consultantId + "/markup-rules",
            Map.of("category", "FLIGHT", "markupType", "PERCENTAGE", "percentageValue", 10), token);

        postJson("/api/v1/itineraries/" + itineraryId + "/line-items/hotel", Map.ofEntries(
            Map.entry("supplierId", "HOTELBEDS"), Map.entry("supplierRateId", "rate-hotel"),
            Map.entry("propertyName", "The Ritz"), Map.entry("roomType", "Deluxe Room"),
            Map.entry("mealPlan", "BB"), Map.entry("cancellationDeadline", Instant.now().plusSeconds(3600).toString()),
            Map.entry("netRate", 5000), Map.entry("netRateCurrency", "GBP"), Map.entry("sellCurrency", "GBP"),
            Map.entry("fxRate", 1), Map.entry("bufferPercent", 3), Map.entry("commissionPercent", 0)), token);
        postJson("/api/v1/itineraries/" + itineraryId + "/line-items/flight", Map.ofEntries(
            Map.entry("supplierId", "MYSTIFLY"), Map.entry("supplierRateId", "rate-flight"),
            Map.entry("airlineCode", "BA"), Map.entry("flightNumber", "BA001"),
            Map.entry("cabinClass", "ECONOMY"), Map.entry("baggageAllowance", "23kg"),
            Map.entry("netRate", 3000), Map.entry("netRateCurrency", "GBP"), Map.entry("sellCurrency", "GBP"),
            Map.entry("fxRate", 1), Map.entry("bufferPercent", 3), Map.entry("commissionPercent", 0)), token);

        String quotationIdJson = postForRawBody("/api/v1/itineraries/" + itineraryId + "/quotation", null, token);
        UUID quotationId = UUID.fromString(stripQuotes(quotationIdJson));

        Map<String, Object> packageResponse = postJson("/api/v1/quotations/" + quotationId + "/package", Map.of(
            "name", "London Flight+Hotel Combo", "description", "Dynamic combo per PRD S17.2",
            "validityStart", Instant.now().toString().substring(0, 10),
            "validityEnd", Instant.now().plusSeconds(90L * 24 * 3600).toString().substring(0, 10),
            "markupPrice", 500, "maxPax", 2), token);
        UUID packageId = UUID.fromString((String) packageResponse.get("packageId"));

        Long dynamicCombo = jdbcTemplate.queryForObject(
            "SELECT CASE WHEN dynamic_flight_hotel_combo THEN 1 ELSE 0 END FROM travel_package WHERE package_id = ?",
            Long.class, packageId);
        assertThat(dynamicCombo).as("a flight+hotel package for a UK consultant must be auto-flagged as a dynamic combo (BOK-11)").isEqualTo(1);

        // BOK-11's real ATOL gate: publish is blocked until disclosure completes.
        var blocked = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.reactive.function.client.WebClientResponseException.class,
            () -> postForRawBody("/api/v1/packages/" + packageId + "/publish", Map.of("promoteViaAds", false), token));
        assertThat(blocked.getStatusCode().value()).isEqualTo(409);

        postForRawBody("/api/v1/packages/" + packageId + "/atol-disclosure", null, token);
        postForRawBody("/api/v1/packages/" + packageId + "/publish", Map.of("promoteViaAds", false), token);

        Map<String, Object> bookingResponse = postJson("/api/v1/bookings",
            Map.of("quotationOrPackageId", packageId.toString(), "totalSellPrice", BigDecimal.valueOf(9350), "currency", "GBP"),
            token);
        UUID bookingId = UUID.fromString((String) bookingResponse.get("bookingId"));

        Map<String, Object> walletAfter = getJson("/api/v1/wallet?consultantId=" + consultantId, token);
        assertThat(new BigDecimal(walletAfter.get("availableBalance").toString()))
            .isEqualByComparingTo(BigDecimal.valueOf(9350).negate());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(emailAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains(bookingId.toString())))
                .isTrue());

        // The gap this test confirms: a real ATOL cert reference is never
        // set, even for a genuinely ATOL-gated, disclosure-completed combo.
        Map<String, Object> voucherRow = jdbcTemplate.queryForMap(
            "SELECT pdf_reference, atol_certificate_reference FROM voucher WHERE booking_id = ?", bookingId);
        assertThat(voucherRow.get("pdf_reference")).asString().contains(bookingId.toString());
        assertThat(voucherRow.get("atol_certificate_reference"))
            .as("KNOWN GAP (confirmed, not assumed): VoucherService.generateFor never populates this for a real UK dynamic combo booking")
            .isNull();
    }

    /** White-label-themed path: a Consultant's real branding update, resolved back over real HTTP. */
    @Test
    void aConsultantsWhiteLabelBrandingRealliesUpdatesAndResolvesOverRealHttp() {
        String token = mintSuperAdminToken();
        UUID consultantId = onboardUkConsultant("Themed Travel Co", token);
        // branding.domain has a real UNIQUE constraint (V7 migration) — this
        // runs against the ambient, persistent docker-compose Postgres (not
        // a fresh Testcontainers instance per run), so a hardcoded domain
        // value collides with a prior run's leftover row on re-run.
        String uniqueDomain = "themed-travel-" + UUID.randomUUID() + ".adren.travel";

        patchJson("/api/v1/consultants/" + consultantId + "/branding", Map.of(
            "logoUrl", "https://cdn.example.com/themed-travel-logo.png",
            "backgroundImageUrl", "https://cdn.example.com/themed-travel-bg.jpg",
            "backgroundColor", "#0B1D3A",
            "textColorPrimary", "#FFFFFF",
            "textColorSecondary", "#C9A227",
            "domain", uniqueDomain), token);

        Map<String, Object> branding = getJson("/api/v1/consultants/" + consultantId + "/branding", token);
        assertThat(branding.get("logoUrl")).isEqualTo("https://cdn.example.com/themed-travel-logo.png");
        assertThat(branding.get("backgroundColor")).isEqualTo("#0B1D3A");
        assertThat(branding.get("domain")).isEqualTo(uniqueDomain);
    }

    private UUID onboardUkConsultant(String businessName, String token) {
        Map<String, Object> response = postJson("/api/v1/consultants",
            Map.of("businessName", businessName, "homeMarket", "UK", "kycFields",
                Map.of("companiesHouseNumber", "CH12345678", "bankDetails", "SORT12-34-56/12345678"),
                "email", "owner-" + UUID.randomUUID() + "@example.com",
                "initialPassword", "InitialPassword1!"),
            token);
        return UUID.fromString((String) response.get("consultantId"));
    }

    private UUID insertDraftItinerary(UUID consultantId) {
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'DRAFT', false, now(), now())",
            itineraryId, consultantId);
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'GBP', now())",
            consultantId);
        return itineraryId;
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

    private void patchJson(String path, Object body, String token) {
        webClient().patch().uri(path)
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

    private static String stripQuotes(String raw) {
        return raw.replace("\"", "");
    }
}
