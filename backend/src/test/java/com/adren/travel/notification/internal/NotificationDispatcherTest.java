package com.adren.travel.notification.internal;

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

/**
 * HRD-01/HRD-02's core acceptance criteria, now centralized on the shared
 * dispatcher every trigger-event listener uses: email always dispatches,
 * and the region-routed secondary channel follows the Consultant's home
 * market, falling back to SMS when the market can't be resolved.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    WhitelabelApi whitelabelApi;

    @Mock
    EmailClient emailClient;

    @Mock
    WhatsAppClient whatsAppClient;

    @Mock
    SmsClient smsClient;

    NotificationDispatcher dispatcher;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
    }

    @Test
    void aDubaiConsultantsDispatchUsesWhatsAppAsTheSecondaryChannel() {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.DUBAI_UAE);

        dispatcher.dispatch(consultantId, "subject", "body", "message");

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(whatsAppClient).send(eq(consultantId), any());
        verify(smsClient, never()).send(any(), any());
    }

    @Test
    void aUkConsultantsDispatchUsesSmsAsTheSecondaryChannel() {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.UK);

        dispatcher.dispatch(consultantId, "subject", "body", "message");

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(smsClient).send(eq(consultantId), any());
        verify(whatsAppClient, never()).send(any(), any());
    }

    @Test
    void anIndiaConsultantsDispatchUsesWhatsAppAsTheSecondaryChannel() {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.INDIA);

        dispatcher.dispatch(consultantId, "subject", "body", "message");

        verify(whatsAppClient).send(eq(consultantId), any());
    }

    @Test
    void emailIsSentRegardlessOfWhichSecondaryChannelApplies() {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.USA);

        dispatcher.dispatch(consultantId, "subject", "body", "message");

        verify(emailClient).send(eq(consultantId), any(), any());
    }

    @Test
    void fallsBackToSmsWhenTheConsultantWasNeverOnboarded() {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId))
            .thenThrow(new IllegalArgumentException("No such consultant: " + consultantId));

        dispatcher.dispatch(consultantId, "subject", "body", "message");

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(smsClient).send(eq(consultantId), any());
        verify(whatsAppClient, never()).send(any(), any());
    }
}
