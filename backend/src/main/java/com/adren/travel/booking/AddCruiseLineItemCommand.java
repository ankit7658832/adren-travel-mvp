package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Adds a Cruise line item to an itinerary (PRD §20.5, §10.2.6, BOK-06).
 * {@code supplierRateId} maps to Widgety's {@code SailingId}; {@code ports}
 * is Widgety's own multi-port itinerary structure flattened into this single
 * line item as metadata (§10.2.6's explicit flattening rule), not modeled as
 * separate line items. {@code passengerDocumentsRequired} drives the
 * Traveler Profile passport requirement (BOK-14) once a cruise line item is
 * added to an itinerary that also needs traveler documents captured.
 */
public record AddCruiseLineItemCommand(
    SupplierId supplierId,
    String supplierRateId,
    String cruiseLine,
    String cabinCategory,
    List<String> ports,
    boolean passengerDocumentsRequired,
    Money netRate,
    CurrencyCode sellCurrency,
    BigDecimal fxRate,
    BigDecimal bufferPercent,
    BigDecimal commissionPercent
) {

    public AddCruiseLineItemCommand {
        Objects.requireNonNull(supplierId, "supplierId must not be null");
        Objects.requireNonNull(supplierRateId, "supplierRateId must not be null");
        Objects.requireNonNull(cruiseLine, "cruiseLine must not be null");
        Objects.requireNonNull(cabinCategory, "cabinCategory must not be null");
        ports = ports != null ? List.copyOf(ports) : List.of();
        Objects.requireNonNull(netRate, "netRate must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        Objects.requireNonNull(fxRate, "fxRate must not be null");
        Objects.requireNonNull(bufferPercent, "bufferPercent must not be null");
        Objects.requireNonNull(commissionPercent, "commissionPercent must not be null");
    }
}
