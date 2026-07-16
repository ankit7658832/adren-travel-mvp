package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a booking is billed to a Consultant's On-Account balance instead of Stripe/Wallet (PRD §21.4/§20.8, FIN-12). */
public record BookingPaidOnAccountEvent(UUID bookingId, UUID consultantId, Money amount) {
}
