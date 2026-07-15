package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Gates {@code confirmBooking} on Stripe webhook receipt (PRD §12.4,
 * FIN-11) — listens for {@code payments.event.StripePaymentSucceededEvent}
 * and confirms the referenced booking via {@link
 * BookingApi#confirmBookingFromPaymentWebhook}, the {@code @PreAuthorize}
 * -free path built for exactly this: an async event listener has no
 * {@code CurrentPrincipal} on its thread, unlike a real user request.
 */
@Component
class StripePaymentConfirmationListener {

    private final BookingApi bookingApi;

    StripePaymentConfirmationListener(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @ApplicationModuleListener
    void on(StripePaymentSucceededEvent event) {
        bookingApi.confirmBookingFromPaymentWebhook(event.bookingReferenceId(), event.consultantId(), event.amount());
    }
}
