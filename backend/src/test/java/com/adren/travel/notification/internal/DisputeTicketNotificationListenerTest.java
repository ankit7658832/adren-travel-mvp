package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.DisputeTicketCreatedEvent;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FIN-16's dispute-ticket AC: email always dispatches, region-routed secondary channel follows the Consultant's home market — same shape as {@link BookingNotificationListenerTest}. */
@ExtendWith(MockitoExtension.class)
class DisputeTicketNotificationListenerTest {

    @Mock
    WhitelabelApi whitelabelApi;

    @Mock
    EmailClient emailClient;

    @Mock
    WhatsAppClient whatsAppClient;

    @Mock
    SmsClient smsClient;

    DisputeTicketNotificationListener listener;

    @Test
    void anIndiaConsultantsDisputeTicketUsesWhatsAppAsTheSecondaryChannel() {
        listener = new DisputeTicketNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.INDIA);
        DisputeTicketCreatedEvent event = new DisputeTicketCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), consultantId, "Wrong room type delivered");

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(whatsAppClient).send(eq(consultantId), any());
        verify(smsClient, never()).send(any(), any());
    }

    @Test
    void aUkConsultantsDisputeTicketUsesSmsAsTheSecondaryChannel() {
        listener = new DisputeTicketNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.UK);
        DisputeTicketCreatedEvent event = new DisputeTicketCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), consultantId, "Missing airport transfer");

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(smsClient).send(eq(consultantId), any());
        verify(whatsAppClient, never()).send(any(), any());
    }

    @Test
    void fallsBackToSmsWhenTheConsultantWasNeverOnboarded() {
        listener = new DisputeTicketNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId))
            .thenThrow(new IllegalArgumentException("No such consultant: " + consultantId));
        DisputeTicketCreatedEvent event = new DisputeTicketCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), consultantId, "Reason");

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(smsClient).send(eq(consultantId), any());
        verify(whatsAppClient, never()).send(any(), any());
    }
}
