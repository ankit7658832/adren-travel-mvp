package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published whenever UK TOMS VAT is calculated on a package's margin component (PRD §12.1 Worked Example D, FIN-18). */
public record UkTomsVatCalculatedEvent(UUID bookingId, UUID consultantId, Money vatAmount, boolean applied) {
}
