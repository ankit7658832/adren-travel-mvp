package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HRD-01's core acceptance criteria: email always dispatches, and the region-routed secondary channel follows the Consultant's home market. */
@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    @Mock
    WhitelabelApi whitelabelApi;

    @Mock
    EmailClient emailClient;

    @Mock
    WhatsAppClient whatsAppClient;

    @Mock
    SmsClient smsClient;

    BookingNotificationListener listener;

    @Test
    void aDubaiConsultantsBookingConfirmationUsesWhatsAppAsTheSecondaryChannel() {
        listener = new BookingNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.DUBAI_UAE);
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(whatsAppClient).send(eq(consultantId), any());
        verify(smsClient, never()).send(any(), any());
    }

    @Test
    void aUkConsultantsBookingConfirmationUsesSmsAsTheSecondaryChannel() {
        listener = new BookingNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.UK);
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(smsClient).send(eq(consultantId), any());
        verify(whatsAppClient, never()).send(any(), any());
    }

    @Test
    void anIndiaConsultantsBookingConfirmationUsesWhatsAppAsTheSecondaryChannel() {
        listener = new BookingNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.INDIA);
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(whatsAppClient).send(eq(consultantId), any());
    }

    @Test
    void emailIsSentRegardlessOfWhichSecondaryChannelApplies() {
        listener = new BookingNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.USA);
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(500), CurrencyCode.USD));

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
    }

    @Test
    void fallsBackToSmsWhenTheConsultantWasNeverOnboarded() {
        listener = new BookingNotificationListener(whitelabelApi, new SecondaryChannelProvider(),
            emailClient, whatsAppClient, smsClient);
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findConsultantMarket(consultantId))
            .thenThrow(new IllegalArgumentException("No such consultant: " + consultantId));
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(emailClient).send(eq(consultantId), any(), any());
        verify(smsClient).send(eq(consultantId), any());
        verify(whatsAppClient, never()).send(any(), any());
    }
}
