package com.adren.travel.payments.event;

import com.adren.travel.payments.FxRateSnapshot;

import java.util.UUID;

/** Published when a booking's FX rate is snapshotted and locked (PRD §12.2, §22.4 T7, FIN-04). */
public record FxRateSnapshotTakenEvent(UUID bookingId, UUID consultantId, FxRateSnapshot snapshot) {
}
