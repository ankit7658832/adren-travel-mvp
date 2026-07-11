package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingConfirmedEvent;
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

    @ApplicationModuleListener
    void on(BookingConfirmedEvent event) {
        // TODO: resolve the Consultant's region-configured notification
        // channel (PRD Section 15) and publish to the SNS topic backing it
        // (LocalStack in dev/test, real SNS in production).
    }
}
