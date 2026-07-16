package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published whenever India GST/TCS is calculated for an outbound package (PRD §12.1 Worked Example C, FIN-17). */
public record IndiaGstTcsCalculatedEvent(UUID bookingId, UUID consultantId, Money gstAmount, Money tcsAmount, boolean applied) {
}
