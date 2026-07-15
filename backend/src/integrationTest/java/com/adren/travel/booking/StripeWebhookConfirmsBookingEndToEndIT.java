package com.adren.travel.booking;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.payments.CreatePaymentIntentCommand;
import com.adren.travel.payments.HandleStripeWebhookCommand;
import com.adren.travel.payments.PaymentIntentView;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full end-to-end integration test for FIN-11's webhook-driven confirmation
 * flow: real Spring Boot context, real Postgres (via Testcontainers) —
 * proves the CROSS-MODULE chain genuinely works, not just each module in
 * isolation: {@code PaymentsApi.createPaymentIntent} →
 * {@code PaymentsApi.handleStripeWebhook} → {@code StripePaymentSucceededEvent}
 * → {@code booking.internal.StripePaymentConfirmationListener} →
 * {@code BookingApi.confirmBookingFromPaymentWebhook} →
 * {@code BookingConfirmedEvent}. {@code @ApplicationModuleTest}'s
 * {@code Scenario} utility isn't available on a plain
 * {@code @SpringBootTest}, so a small test-registered listener plus
 * Awaitility (same approach as {@code NotificationTraceIdPropagationTest})
 * observes the final event instead.
 * <p>
 * Requires Docker to be available on the host/CI runner — see
 * {@code BookingEndToEndIT}/{@code PricingPipelineEndToEndIT} for the same
 * tier/shape.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestInfrastructure.class)
class StripeWebhookConfirmsBookingEndToEndIT {

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

    // Field injection is fine for same-module/framework types, but Spring
    // Modulith's own architecture check flags field injection of ANOTHER
    // application module's type from outside it (see
    // BookingModuleIntegrationTests's identical fix) — constructor
    // injection here for the same reason.
    final PaymentsApi paymentsApi;

    @Autowired
    StripeWebhookConfirmsBookingEndToEndIT(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @Autowired
    AtomicReference<BookingConfirmedEvent> capturedBookingConfirmedEvent;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aSucceededStripeWebhookConfirmsTheReferencedBookingEndToEnd() {
        UUID bookingReferenceId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.GBP);

        PaymentIntentView intent = paymentsApi.createPaymentIntent(
            new CreatePaymentIntentCommand(bookingReferenceId, consultantId, amount));

        paymentsApi.handleStripeWebhook(new HandleStripeWebhookCommand("payment_intent.succeeded", intent.paymentIntentId()));

        Awaitility.await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(capturedBookingConfirmedEvent.get()).isNotNull());
        assertThat(capturedBookingConfirmedEvent.get().consultantId()).isEqualTo(consultantId);
        assertThat(capturedBookingConfirmedEvent.get().totalSellPrice()).isEqualTo(amount);
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
