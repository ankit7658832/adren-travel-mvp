package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Adds an Activity line item to an itinerary (PRD §20.6, §10.2.7, BOK-07).
 * {@code supplierRateId} maps to HBActivities' {@code ActivityId};
 * {@code timeSlot} is a specific time (not a date range) since activity
 * availability is time-slot based, per §10.2.7. {@code headcount} is fixed
 * at booking per most supplier contracts (§10.2.7) — see {@code
 * ActivityLineItem#updateHeadcount} for where that immutability is enforced.
 */
public record AddActivityLineItemCommand(
    SupplierId supplierId,
    String supplierRateId,
    int durationMinutes,
    LocalTime timeSlot,
    int headcount,
    Money netRate,
    CurrencyCode sellCurrency,
    BigDecimal fxRate,
    BigDecimal bufferPercent,
    BigDecimal commissionPercent
) {

    public AddActivityLineItemCommand {
        Objects.requireNonNull(supplierId, "supplierId must not be null");
        Objects.requireNonNull(supplierRateId, "supplierRateId must not be null");
        Objects.requireNonNull(timeSlot, "timeSlot must not be null");
        Objects.requireNonNull(netRate, "netRate must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        Objects.requireNonNull(fxRate, "fxRate must not be null");
        Objects.requireNonNull(bufferPercent, "bufferPercent must not be null");
        Objects.requireNonNull(commissionPercent, "commissionPercent must not be null");
        if (headcount <= 0) {
            throw new IllegalArgumentException("headcount must be a positive value");
        }
    }
}
