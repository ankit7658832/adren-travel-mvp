package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Published once a Stripe webhook confirms a PaymentIntent succeeded (PRD
 * §12.4, FIN-11). {@code bookingReferenceId} is the quotation/package id
 * {@code CreatePaymentIntentCommand} was created against — the Booking
 * module listens for this to gate {@code confirmBooking} on webhook
 * receipt rather than confirming on submission alone.
 */
public record StripePaymentSucceededEvent(UUID bookingReferenceId, UUID consultantId, Money amount) {
}
