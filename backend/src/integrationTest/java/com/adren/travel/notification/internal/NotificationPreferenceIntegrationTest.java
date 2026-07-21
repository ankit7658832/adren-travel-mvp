package com.adren.travel.notification.internal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.notification.NotificationApi;
import com.adren.travel.notification.NotificationPreferenceView;
import com.adren.travel.notification.UpdateNotificationPreferenceCommand;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-04's own acceptance criteria, end to end against real Postgres: the
 * regional default is pre-selected until a Consultant saves an override,
 * and once saved, {@code NotificationDispatcher} — and so every listener,
 * not just a hand-picked one — actually uses it. Same slice shape as
 * {@code NotificationRegionRoutingIntegrationTest}.
 */
@ApplicationModuleTest(
    value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"booking", "payments", "supplier", "whitelabel", "security", "ai"})
class NotificationPreferenceIntegrationTest {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    private final WhitelabelApi whitelabelApi;
    private final BookingApi bookingApi;
    private final NotificationApi notificationApi;

    @Autowired
    NotificationPreferenceIntegrationTest(WhitelabelApi whitelabelApi, BookingApi bookingApi, NotificationApi notificationApi) {
        this.whitelabelApi = whitelabelApi;
        this.bookingApi = bookingApi;
        this.notificationApi = notificationApi;
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
    void anIndiaConsultantsSmsOverrideIsUsedInsteadOfTheWhatsAppMarketDefault(Scenario scenario) {
        startCapturing();
        UUID consultantId = onboardAsSuperAdmin(Market.INDIA);

        authenticateAs(Role.CONSULTANT, consultantId);
        NotificationPreferenceView beforeOverride = notificationApi.findNotificationPreference();
        assertThat(beforeOverride.secondaryChannel()).isEqualTo("WHATSAPP");
        assertThat(beforeOverride.isOverride()).isFalse();

        notificationApi.updateNotificationPreference(new UpdateNotificationPreferenceCommand("SMS"));
        NotificationPreferenceView afterOverride = notificationApi.findNotificationPreference();
        assertThat(afterOverride.secondaryChannel()).isEqualTo("SMS");
        assertThat(afterOverride.isOverride()).isTrue();

        UUID quotationId = insertQuotationForANewDraftItinerary(consultantId);
        SecurityContextHolder.clearContext();
        authenticateAsSuperAdmin();
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

    private static List<ILoggingEvent> messagesFor(ListAppender<ILoggingEvent> appender, UUID consultantId) {
        return appender.list.stream()
            .filter(event -> event.getFormattedMessage().contains(consultantId.toString()))
            .toList();
    }

    private UUID onboardAsSuperAdmin(Market market) {
        authenticateAsSuperAdmin();
        UUID consultantId = whitelabelApi.onboardConsultant(new OnboardConsultantCommand(
            "India Co", market, Map.of("gstRegistration", "GST1", "businessPan", "PAN1", "bankDetails", "x"),
            "owner-" + UUID.randomUUID() + "@example.com", "InitialPassword1!"));
        SecurityContextHolder.clearContext();
        return consultantId;
    }

    private UUID insertQuotationForANewDraftItinerary(UUID consultantId) {
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'INR', now())",
            consultantId);
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'QUOTATION', false, now(), now())",
            itineraryId, consultantId);
        UUID quotationId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO quotation (quotation_id, itinerary_id, valid_until, shared_with_traveler, created_at) " +
                "VALUES (?, ?, now() + interval '7 days', false, now())",
            quotationId, itineraryId);
        return quotationId;
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private static void authenticateAsSuperAdmin() {
        authenticateAs(Role.SUPER_ADMIN, null);
    }
}
