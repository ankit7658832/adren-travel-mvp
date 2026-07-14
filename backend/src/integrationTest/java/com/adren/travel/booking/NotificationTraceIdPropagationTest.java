package com.adren.travel.booking;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.event.BookingConfirmedEvent;
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
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
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
 * listener in alongside booking's own full scan.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "notification")
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

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.BookingNotificationListener");
        logger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void listenerLogLineCarriesTheSameTraceIdAsTheTriggeringRequest(Scenario scenario) {
        Logger logger = (Logger) LoggerFactory.getLogger("com.adren.travel.notification.internal.BookingNotificationListener");
        appender.start();
        logger.addAppender(appender);

        String requestTraceId = "trace-" + UUID.randomUUID();
        MDC.put(TraceIds.MDC_KEY, requestTraceId);

        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        scenario.stimulate(() -> bookingApi.confirmBooking(UUID.randomUUID(), price))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .toArrive();

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(appender.list).isNotEmpty());

        String listenerTraceId = appender.list.get(0).getMDCPropertyMap().get(TraceIds.MDC_KEY);
        assertThat(listenerTraceId).isEqualTo(requestTraceId);
    }
}
