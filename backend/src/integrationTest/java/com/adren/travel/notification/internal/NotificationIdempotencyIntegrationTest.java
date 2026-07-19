package com.adren.travel.notification.internal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-03 / RULES.md §2.2's own required proof: a crash-and-retry
 * redelivery of the exact same event to the exact same listener must not
 * double-notify. Calls the package-private listener directly twice with
 * the SAME event instance (Modulith's own redelivery mechanism ultimately
 * does the same thing — re-invoke the listener method with the originally
 * published payload) rather than round-tripping through the real async
 * event bus, which would make "did it run twice" non-deterministic to
 * assert on.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "security")
class NotificationIdempotencyIntegrationTest {

    @Autowired
    BookingNotificationListener listener;

    @Autowired
    ProcessedEventRepository processedEventRepository;

    private final ListAppender<ILoggingEvent> emailAppender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(StubEmailClient.class)).detachAppender(emailAppender);
    }

    @Test
    void aRedeliveredBookingConfirmedEventSendsOnlyOneEmail() {
        emailAppender.start();
        ((Logger) LoggerFactory.getLogger(StubEmailClient.class)).addAppender(emailAppender);
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            bookingId, consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);
        listener.on(event); // simulated crash-and-retry redelivery of the SAME event

        // BookingNotificationListener is @Async (via @ApplicationModuleListener) —
        // even called directly, the proxy dispatches to a thread pool, so
        // the dispatch(es) may not have completed yet at this point.
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            List<ILoggingEvent> emailsForThisBooking = emailAppender.list.stream()
                .filter(e -> e.getFormattedMessage().contains(bookingId.toString()))
                .toList();
            assertThat(emailsForThisBooking).hasSize(1);
        });

        long processedRows = processedEventRepository.findAll().stream()
            .filter(row -> row.getEventId().equals(bookingId.toString())
                && row.getListenerName().equals("BookingNotificationListener"))
            .count();
        assertThat(processedRows).isEqualTo(1);
    }
}
