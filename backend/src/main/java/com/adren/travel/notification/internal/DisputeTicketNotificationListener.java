package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.DisputeTicketCreatedEvent;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * PRD §12.5, FIN-16 — "a dispute create a tracked ticket, not just an
 * email handoff": the ticket ({@code DisputeTicket}, booking module) IS
 * the tracked record; this listener is the notification fan-out on top of
 * it, alerting the Consultant a ticket now exists — same region-routed
 * email + WhatsApp/SMS shape {@link BookingNotificationListener}
 * establishes for HRD-01, reused here rather than duplicated.
 */
@Component
class DisputeTicketNotificationListener {

    private final WhitelabelApi whitelabelApi;
    private final SecondaryChannelProvider secondaryChannelProvider;
    private final EmailClient emailClient;
    private final WhatsAppClient whatsAppClient;
    private final SmsClient smsClient;

    DisputeTicketNotificationListener(WhitelabelApi whitelabelApi, SecondaryChannelProvider secondaryChannelProvider,
                                       EmailClient emailClient, WhatsAppClient whatsAppClient, SmsClient smsClient) {
        this.whitelabelApi = whitelabelApi;
        this.secondaryChannelProvider = secondaryChannelProvider;
        this.emailClient = emailClient;
        this.whatsAppClient = whatsAppClient;
        this.smsClient = smsClient;
    }

    @ApplicationModuleListener
    void on(DisputeTicketCreatedEvent event) {
        String subject = "A dispute has been flagged on your booking";
        String body = "Dispute ticket " + event.disputeTicketId() + " was opened for booking "
            + event.bookingId() + ": " + event.reason();
        emailClient.send(event.consultantId(), subject, body);

        NotificationChannel secondaryChannel = resolveSecondaryChannel(event.consultantId());
        String message = "Dispute opened for booking " + event.bookingId() + ".";
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
            // Same defensive fallback as BookingNotificationListener — this
            // listener must never be able to fail the flagDispute flow it
            // reacts to.
            return NotificationChannel.SMS;
        }
    }
}
