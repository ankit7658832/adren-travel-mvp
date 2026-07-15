package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** FIN-11 — proves the listener confirms the booking through the @PreAuthorize-free webhook path, not confirmBooking. */
class StripePaymentConfirmationListenerTest {

    private final BookingApi bookingApi = mock(BookingApi.class);
    private final StripePaymentConfirmationListener listener = new StripePaymentConfirmationListener(bookingApi);

    @Test
    void confirmsTheReferencedBookingThroughTheWebhookPath() {
        UUID bookingReferenceId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        var event = new StripePaymentSucceededEvent(bookingReferenceId, consultantId, amount);

        listener.on(event);

        verify(bookingApi).confirmBookingFromPaymentWebhook(bookingReferenceId, consultantId, amount);
    }
}
