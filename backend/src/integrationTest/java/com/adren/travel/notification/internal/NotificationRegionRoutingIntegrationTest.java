package com.adren.travel.notification.internal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-01's actual acceptance criteria end to end: a real onboarded
 * Consultant's home market drives which secondary channel a booking
 * confirmation goes out on (WhatsApp for Dubai, SMS for UK), proven through
 * the real event-publication registry rather than a mocked
 * {@code WhitelabelApi}. Lives in {@code notification.internal} (not
 * {@code notification}) so it can attach log capture directly to the
 * package-private {@code StubWhatsAppClient}/{@code StubSmsClient} loggers
 * — there is no other observable side effect from these stubs.
 * {@code extraIncludes} widens the slice to {@code booking}'s own full
 * dependency tree ({@code payments}, {@code supplier}, {@code whitelabel})
 * plus {@code security} — unlike {@code BookingModuleIntegrationTests},
 * where {@code booking} itself is the module under test so
 * {@code DIRECT_DEPENDENCIES} cascades into its dependencies automatically,
 * here {@code notification} is the primary module, so booking's own
 * dependencies must be named explicitly. Mirrors
 * {@code NotificationTraceIdPropagationTest}'s inverse-direction setup.
 */
@ApplicationModuleTest(
    value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"booking", "payments", "supplier", "whitelabel", "security"})
class NotificationRegionRoutingIntegrationTest {

    /**
     * {@code @ApplicationModuleTest}'s slice doesn't auto-configure
     * {@code WebClient.Builder} the way a full {@code @SpringBootTest}
     * would — {@code supplier}'s {@code HotelbedsClient} needs one.
     */
    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    private final WhitelabelApi whitelabelApi;
    private final BookingApi bookingApi;

    @Autowired
    NotificationRegionRoutingIntegrationTest(WhitelabelApi whitelabelApi, BookingApi bookingApi) {
        this.whitelabelApi = whitelabelApi;
        this.bookingApi = bookingApi;
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ListAppender<ILoggingEvent> whatsAppAppender = new ListAppender<>();
    private final ListAppender<ILoggingEvent> smsAppender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(StubWhatsAppClient.class)).detachAppender(whatsAppAppender);
        ((Logger) LoggerFactory.getLogger(StubSmsClient.class)).detachAppender(smsAppender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void aDubaiConsultantsBookingConfirmationIsRoutedToWhatsApp(Scenario scenario) {
        startCapturing();
        authenticateAsSuperAdmin();
        UUID consultantId = whitelabelApi.onboardConsultant(new OnboardConsultantCommand(
            "Dubai Co", Market.DUBAI_UAE, Map.of("dtcmTradeLicense", "DTCM1", "bankDetails", "x")));
        UUID quotationId = insertQuotationForANewDraftItinerary(consultantId);

        scenario.stimulate(() -> bookingApi.confirmBooking(quotationId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR)))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .toArrive();

        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(messagesFor(whatsAppAppender, consultantId)).isNotEmpty());
        assertThat(messagesFor(smsAppender, consultantId)).isEmpty();
    }

    @Test
    void aUkConsultantsBookingConfirmationIsRoutedToSms(Scenario scenario) {
        startCapturing();
        authenticateAsSuperAdmin();
        UUID consultantId = whitelabelApi.onboardConsultant(new OnboardConsultantCommand(
            "UK Co", Market.UK, Map.of("companiesHouseNumber", "CH1", "bankDetails", "x")));
        UUID quotationId = insertQuotationForANewDraftItinerary(consultantId);

        scenario.stimulate(() -> bookingApi.confirmBooking(quotationId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR)))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .toArrive();

        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(messagesFor(smsAppender, consultantId)).isNotEmpty());
        assertThat(messagesFor(whatsAppAppender, consultantId)).isEmpty();
    }

    private void startCapturing() {
        whatsAppAppender.start();
        smsAppender.start();
        ((Logger) LoggerFactory.getLogger(StubWhatsAppClient.class)).addAppender(whatsAppAppender);
        ((Logger) LoggerFactory.getLogger(StubSmsClient.class)).addAppender(smsAppender);
    }

    // Both tests share one cached Spring context (JUnit's TestContext
    // caching), and this listener is @Async, so a slow/delayed delivery
    // from the OTHER test's stimulus can still land in a still-attached
    // static logger's appender after this test's own event completes.
    // Filtering by this test's own consultantId (rather than asserting the
    // whole list is empty) keeps each test's assertions scoped to the event
    // it actually triggered.
    private static List<ILoggingEvent> messagesFor(ListAppender<ILoggingEvent> appender, UUID consultantId) {
        return appender.list.stream()
            .filter(event -> event.getFormattedMessage().contains(consultantId.toString()))
            .toList();
    }

    // BOK-13: confirmBooking resolves a real consultantId from the
    // quotation/package it's given, so the itinerary here must carry the
    // SAME consultantId as the just-onboarded Consultant for
    // WhitelabelApi.findConsultantMarket to resolve a real market rather
    // than fall back to the defensive SMS default.
    private UUID insertQuotationForANewDraftItinerary(UUID consultantId) {
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'DRAFT', false, now(), now())",
            itineraryId, consultantId);
        UUID quotationId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO quotation (quotation_id, itinerary_id, valid_until, shared_with_traveler, created_at) " +
                "VALUES (?, ?, now() + interval '7 days', false, now())",
            quotationId, itineraryId);
        return quotationId;
    }

    private static void authenticateAsSuperAdmin() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.SUPER_ADMIN, null);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
