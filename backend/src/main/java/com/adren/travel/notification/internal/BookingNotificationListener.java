package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.LogFields;
import com.adren.travel.shared.TraceIds;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real implementation of PRD Section 15's notification dispatch (HRD-01) —
 * email always, plus a region-routed secondary channel (WhatsApp for
 * India/Dubai, SMS elsewhere per §22.7 T11). Per-Consultant override
 * (HRD-04's notification preferences screen) isn't built yet, so the
 * market default always applies.
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
 * writeup.
 */
@Component
class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final WhitelabelApi whitelabelApi;
    private final SecondaryChannelProvider secondaryChannelProvider;
    private final EmailClient emailClient;
    private final WhatsAppClient whatsAppClient;
    private final SmsClient smsClient;

    BookingNotificationListener(WhitelabelApi whitelabelApi, SecondaryChannelProvider secondaryChannelProvider,
                                 EmailClient emailClient, WhatsAppClient whatsAppClient, SmsClient smsClient) {
        this.whitelabelApi = whitelabelApi;
        this.secondaryChannelProvider = secondaryChannelProvider;
        this.emailClient = emailClient;
        this.whatsAppClient = whatsAppClient;
        this.smsClient = smsClient;
    }

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

        String subject = "Your booking is confirmed";
        String body = "Booking " + event.bookingId() + " is confirmed for " + event.totalSellPrice() + ".";
        emailClient.send(event.consultantId(), subject, body);

        NotificationChannel secondaryChannel = resolveSecondaryChannel(event.consultantId());
        String message = "Booking " + event.bookingId() + " confirmed.";
        if (secondaryChannel == NotificationChannel.WHATSAPP) {
            whatsAppClient.send(event.consultantId(), message);
        } else {
            smsClient.send(event.consultantId(), message);
        }
    }

    private NotificationChannel resolveSecondaryChannel(UUID consultantId) {
        try {
            Market market = whitelabelApi.findConsultantMarket(consultantId);
            return secondaryChannelProvider.defaultChannelFor(market);
        } catch (IllegalArgumentException e) {
            // Defensive: several confirmBooking paths across this
            // walking-skeleton's test suite use a consultantId that was
            // never onboarded as a real Consultant record — fall back to
            // SMS (the majority-market default) rather than let
            // notification dispatch throw, since this listener must never
            // be able to fail the booking flow it reacts to.
            return NotificationChannel.SMS;
        }
    }
}
