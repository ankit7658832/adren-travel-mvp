package com.adren.travel.notification.internal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.CalculateCancellationRefundCommand;
import com.adren.travel.booking.event.BookingCancelledEvent;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-05's own acceptance criterion, end to end against real Postgres: a
 * penalty-free cancellation completes without an explicit approval step,
 * and the notification fires on refund completion. Same slice shape as
 * {@code NotificationRegionRoutingIntegrationTest} — {@code notification}
 * is the module under test, so {@code booking}'s own dependencies are
 * named explicitly via {@code extraIncludes}.
 */
@ApplicationModuleTest(
    value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"booking", "payments", "supplier", "whitelabel", "security", "ai"})
class NotificationCancellationIntegrationTest {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    private final BookingApi bookingApi;
    private final BookingCancelledNotificationListener listener;

    @Autowired
    NotificationCancellationIntegrationTest(BookingApi bookingApi, BookingCancelledNotificationListener listener) {
        this.bookingApi = bookingApi;
        this.listener = listener;
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ListAppender<ILoggingEvent> emailAppender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(StubEmailClient.class)).detachAppender(emailAppender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void aPenaltyFreeCancellationCompletesWithoutApprovalAndNotifiesOnRefundHRD05() {
        emailAppender.start();
        ((Logger) LoggerFactory.getLogger(StubEmailClient.class)).addAppender(emailAppender);

        UUID consultantId = UUID.randomUUID();
        UUID bookingId = insertConfirmedBooking(consultantId);
        authenticateAsSuperAdmin();
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));
        var command = new CalculateCancellationRefundCommand(new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR),
            Instant.now().plusSeconds(3600), Instant.now(), BigDecimal.valueOf(30), originalFxRateSnapshot);

        var result = bookingApi.submitCancellation(bookingId, command);

        // HRD-05 AC: completes without requiring explicit approval.
        assertThat(result.status()).isEqualTo("REFUNDED");

        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(emailAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains(consultantId.toString())
                    && e.getFormattedMessage().contains(bookingId.toString())))
                .isTrue());
    }

    /**
     * HRD-03 Step C — Hardening's own before/after requirement: proves the
     * idempotency guard survives the actual condition it exists for
     * (Spring Modulith's documented at-least-once redelivery), against the
     * real {@code processed_event} table and its unique constraint — not a
     * mocked {@code ProcessedEventDeduplicationService}. Submits one real
     * cancellation (the event fires once through the async listener), then
     * manually re-delivers an equal {@link BookingCancelledEvent} straight
     * to the same listener bean — exactly what a redelivery after a crash
     * between "process" and "acknowledge" looks like. Asserts exactly one
     * notification total, not two.
     */
    @Test
    void aRedeliveredCancellationEventDoesNotDoubleDispatchTheNotificationHRD03() {
        emailAppender.start();
        ((Logger) LoggerFactory.getLogger(StubEmailClient.class)).addAppender(emailAppender);

        UUID consultantId = UUID.randomUUID();
        UUID bookingId = insertConfirmedBooking(consultantId);
        authenticateAsSuperAdmin();
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));
        var command = new CalculateCancellationRefundCommand(new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR),
            Instant.now().plusSeconds(3600), Instant.now(), BigDecimal.valueOf(30), originalFxRateSnapshot);

        var result = bookingApi.submitCancellation(bookingId, command);
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(messagesFor(bookingId)).hasSize(1));

        // Simulate redelivery: the same event, delivered a second time.
        // The autowired reference is still the @Async proxy, not a raw
        // instance, so this redelivery — like the original — completes on
        // its own async thread; a delayed re-check (not an immediate
        // assertion) is required to actually observe whether it lands.
        listener.on(new BookingCancelledEvent(bookingId, consultantId, result.refundAmount(), result.penaltyAmount()));

        org.awaitility.Awaitility.await()
            .pollDelay(java.time.Duration.ofSeconds(2))
            .atMost(java.time.Duration.ofSeconds(4))
            .untilAsserted(() -> assertThat(messagesFor(bookingId)).hasSize(1));
    }

    private List<ILoggingEvent> messagesFor(UUID bookingId) {
        return emailAppender.list.stream()
            .filter(e -> e.getFormattedMessage().contains(bookingId.toString()))
            .toList();
    }

    private UUID insertConfirmedBooking(UUID consultantId) {
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 20000, 0, 0, 'INR', now())",
            consultantId);
        UUID bookingId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO booking (booking_id, itinerary_id, consultant_id, status, total_sell_price, " +
                "total_sell_currency, payment_method, pnr_searchable_ref, created_at) " +
                "VALUES (?, NULL, ?, 'CONFIRMED', 11500, 'INR', 'WALLET', ?, now())",
            bookingId, consultantId, "PNR" + bookingId.toString().substring(0, 6).toUpperCase());
        return bookingId;
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
