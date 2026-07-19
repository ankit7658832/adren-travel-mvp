package com.adren.travel.ads;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One itemized spend increment (PRD §14.3's billing-transparency requirement, ADS-11) — cross-module-safe, never the JPA entity itself. */
public record SpendTransactionView(UUID transactionId, BigDecimal amount, Instant recordedAt) {
}
