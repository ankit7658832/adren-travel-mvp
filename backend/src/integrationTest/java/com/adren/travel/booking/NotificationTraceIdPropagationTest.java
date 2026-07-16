package com.adren.travel.booking;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.TraceIds;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FND-21's actual acceptance criterion — the same traceId appears in the
 * request thread's log context and the async
 * {@code BookingNotificationListener}'s log line it triggers — proven end
 * to end (real event publication registry, real {@code @Async} executor)
 * rather than only at the {@code MdcTaskDecorator} unit-test level.
 * <p>
 * Lives in {@code booking} (not {@code notification}) with
 * {@code extraIncludes = "notification"}: a module test's component scan
 * only widens to a dependency module's actually-referenced named
 * interface (here, {@code booking.event}), not its whole package tree, so
 * a test scoped to {@code notification} alone cannot resolve a
 * {@code BookingApi} bean — {@code extraIncludes} is what pulls the
 * listener in alongside booking's own full scan. {@code security} is also
 * named explicitly for the same reason {@code BookingModuleIntegrationTests}
 * needs it (FND-05/FND-09): {@code whitelabel.internal.WhitelabelServiceImpl}
 * — pulled in because {@code booking} depends on {@code whitelabel.WhitelabelApi}
 * — has its own constructor dependency on {@code security.CapabilityGrantService},
 * a second-degree edge DIRECT_DEPENDENCIES doesn't follow on its own.
 */
@ApplicationModuleTest(
    value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = {"notification", "security"})
class NotificationTraceIdPropagationTest {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    private final BookingApi bookingApi;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @Autowired
    NotificationTraceIdPropagationTest(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.BookingNotificationListener");
        logger.detachAppender(appender);
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void listenerLogLineCarriesTheSameTraceIdAsTheTriggeringRequest(Scenario scenario) {
        Logger logger = (Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.BookingNotificationListener");
        appender.start();
        logger.addAppender(appender);
        // FND-05's tenant-active gate needs an authenticated principal;
        // SUPER_ADMIN (no consultantId) skips it so this test stays
        // focused on traceId propagation, not tenant status.
        authenticateAsSuperAdmin();

        String requestTraceId = "trace-" + UUID.randomUUID();
        MDC.put(TraceIds.MDC_KEY, requestTraceId);

        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        UUID quotationId = insertQuotationForANewDraftItinerary();
        scenario.stimulate(() -> bookingApi.confirmBooking(quotationId, price))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .toArrive();

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(appender.list).isNotEmpty());

        String listenerTraceId = appender.list.get(0).getMDCPropertyMap().get(TraceIds.MDC_KEY);
        assertThat(listenerTraceId).isEqualTo(requestTraceId);
    }

    // BOK-13: confirmBooking now resolves a real consultantId from the
    // quotation/package it's given, so this test needs a genuinely
    // persisted Quotation (+ its Itinerary) rather than a random UUID.
    // BOK-16: confirmBooking now requires the itinerary to be QUOTATION
    // (markAsBooked()'s precondition), not DRAFT — this helper bypasses the
    // real saveAsQuotation transition by inserting rows directly, so it
    // must insert the post-transition status itself.
    private UUID insertQuotationForANewDraftItinerary() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'QUOTATION', false, now(), now())",
            itineraryId, consultantId);
        // FIN-08: confirmBooking's wallet path now enforces the credit limit.
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'INR', now())",
            consultantId);
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
