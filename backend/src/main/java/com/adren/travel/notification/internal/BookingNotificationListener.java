package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.LogFields;
import com.adren.travel.shared.TraceIds;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reference implementation of the event-driven pattern required by the PRD
 * (Section 15 — notifications fire off the {@code BookingConfirmedEvent},
 * not a direct call from the Booking module).
 * <p>
 * {@code @ApplicationModuleListener} (Spring Modulith) is a meta-annotation
 * for {@code @Async @TransactionalEventListener(phase = AFTER_COMMIT)} —
 * meaning:
 * <ul>
 *   <li>this only runs after the booking transaction has committed
 *       successfully (never on a rolled-back booking)</li>
 *   <li>it runs asynchronously, so a slow or failing notification provider
 *       can never block or fail the booking itself</li>
 *   <li>combined with the JPA event publication registry (build.gradle.kts),
 *       a failed listener is retried rather than silently dropped</li>
 * </ul>
 * This is the pattern every cross-module listener in this codebase should
 * follow — see the {@code backend-spring-modulith} skill for the full
 * writeup and the LocalStack SNS/SQS wiring for actually sending the
 * WhatsApp/SMS/email notification (PRD Section 15's region-configurable
 * channel logic).
 */
@Component
class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    @ApplicationModuleListener
    void on(BookingConfirmedEvent event) {
        // FND-24: consultantId and currency go in MDC (not just the message
        // text) so this line is auditable as structured fields, per
        // RULES.md §6.2 — a monetary log line is never just a bare number.
        // putCloseable auto-removes on close, scoping these fields to this
        // one log statement rather than leaking into whatever runs next on
        // this (pooled) async thread.
        try (var consultantScope = MDC.putCloseable(LogFields.CONSULTANT_ID, event.consultantId().toString());
             var currencyScope = MDC.putCloseable(LogFields.CURRENCY, event.totalSellPrice().currency().name())) {
            // FND-21: this log line's traceId must match the request that
            // triggered confirmBooking(), proving MdcTaskDecorator carried MDC
            // context across the @Async executor boundary — see
            // NotificationTraceIdPropagationTest.
            log.info("Booking confirmed notification stub invoked, bookingId={}, totalSellPrice={}, traceId={}",
                event.bookingId(), event.totalSellPrice(), MDC.get(TraceIds.MDC_KEY));
        }
        // TODO: resolve the Consultant's region-configured notification
        // channel (PRD Section 15) and publish to the SNS topic backing it
        // (LocalStack in dev/test, real SNS in production).
    }
}
