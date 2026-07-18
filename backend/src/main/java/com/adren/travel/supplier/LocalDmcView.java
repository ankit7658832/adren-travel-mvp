package com.adren.travel.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A Local DMC record (PRD §10.3, §20.14, DMC-01/02/04/05/11) — never the JPA entity itself. */
public record LocalDmcView(
    UUID localDmcId,
    UUID consultantId,
    String businessName,
    List<String> productCategories,
    String sampleRatesSummary,
    String referencesInfo,
    String status,
    String verificationNotes,
    BigDecimal cancellationRate,
    int complaintCount,
    boolean flagged,
    boolean inventoryStale,
    Instant createdAt
) {
}
