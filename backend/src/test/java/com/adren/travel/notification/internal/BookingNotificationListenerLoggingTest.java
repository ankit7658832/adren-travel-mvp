package com.adren.travel.notification.internal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.LogFields;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * FND-24 — proves the notification listener's monetary log line carries
 * {@code consultantId} and {@code currency} as structured MDC fields (not
 * just embedded in the message text), and that both are cleared once the
 * log statement's try-with-resources scope ends so they don't leak onto
 * whatever this pooled thread logs next. {@link NotificationDispatcher} is
 * mocked here since this test is about the log line's structured fields
 * (HRD-01's region-routing logic has its own dedicated test on
 * {@code NotificationDispatcher}).
 */
class BookingNotificationListenerLoggingTest {

    private final NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
    private final BookingNotificationListener listener = new BookingNotificationListener(dispatcher);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(BookingNotificationListener.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void theMonetaryLogLineCarriesConsultantIdAndCurrencyAsStructuredFields() {
        UUID consultantId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent logEvent = appender.list.get(0);
        assertThat(logEvent.getMDCPropertyMap())
            .containsEntry(LogFields.CONSULTANT_ID, consultantId.toString())
            .containsEntry(LogFields.CURRENCY, CurrencyCode.INR.name());
    }

    @Test
    void theStructuredFieldsDoNotLeakOutsideTheLogStatementScope() {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), UUID.randomUUID(), new Money(BigDecimal.valueOf(500), CurrencyCode.USD));

        listener.on(event);

        assertThat(org.slf4j.MDC.get(LogFields.CONSULTANT_ID)).isNull();
        assertThat(org.slf4j.MDC.get(LogFields.CURRENCY)).isNull();
    }
}
