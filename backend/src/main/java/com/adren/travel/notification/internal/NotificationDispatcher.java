package com.adren.travel.notification.internal;

import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * HRD-02 — the "email always, plus a region-routed secondary channel"
 * dispatch shape every trigger-event listener needs (PRD §15/§22.7 T11),
 * extracted once this story adds the fourth/fifth/sixth listener that
 * would otherwise duplicate {@code BookingNotificationListener}'s and
 * {@code DisputeTicketNotificationListener}'s identical
 * resolveSecondaryChannel/dual-send logic — both were refactored onto this
 * shared collaborator in the same change, per backend-best-practices §4's
 * "a growing set of near-identical call sites is the decomposition
 * signal."
 */
@Component
class NotificationDispatcher {

    private final WhitelabelApi whitelabelApi;
    private final SecondaryChannelProvider secondaryChannelProvider;
    private final EmailClient emailClient;
    private final WhatsAppClient whatsAppClient;
    private final SmsClient smsClient;

    NotificationDispatcher(WhitelabelApi whitelabelApi, SecondaryChannelProvider secondaryChannelProvider,
                           EmailClient emailClient, WhatsAppClient whatsAppClient, SmsClient smsClient) {
        this.whitelabelApi = whitelabelApi;
        this.secondaryChannelProvider = secondaryChannelProvider;
        this.emailClient = emailClient;
        this.whatsAppClient = whatsAppClient;
        this.smsClient = smsClient;
    }

    void dispatch(UUID consultantId, String subject, String emailBody, String secondaryMessage) {
        emailClient.send(consultantId, subject, emailBody);

        NotificationChannel secondaryChannel = resolveSecondaryChannel(consultantId);
        if (secondaryChannel == NotificationChannel.WHATSAPP) {
            whatsAppClient.send(consultantId, secondaryMessage);
        } else {
            smsClient.send(consultantId, secondaryMessage);
        }
    }

    private NotificationChannel resolveSecondaryChannel(UUID consultantId) {
        try {
            Market market = whitelabelApi.findConsultantMarket(consultantId);
            return secondaryChannelProvider.defaultChannelFor(market);
        } catch (IllegalArgumentException e) {
            // Defensive: several booking/payments/ai flows across this
            // walking-skeleton's test suite use a consultantId that was
            // never onboarded as a real Consultant record — fall back to
            // SMS (the majority-market default) rather than let
            // notification dispatch throw, since a listener must never be
            // able to fail the flow it reacts to.
            return NotificationChannel.SMS;
        }
    }
}
