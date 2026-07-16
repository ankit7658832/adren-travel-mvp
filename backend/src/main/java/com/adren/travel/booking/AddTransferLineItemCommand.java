package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Adds a Transfer line item to an itinerary (PRD §20.4, §10.2.5, BOK-05).
 * {@code supplierRateId} maps to Transferz's {@code TransferOptionId};
 * {@code pickupPoint}/{@code dropoffPoint} are stored as plain
 * location-code/name strings (no dedicated geocoded-location-entry entity
 * exists elsewhere in this codebase yet to link against, matching {@link
 * AddHotelLineItemCommand}'s equivalent level of fidelity for its own
 * location fields).
 */
public record AddTransferLineItemCommand(
    SupplierId supplierId,
    String supplierRateId,
    String vehicleType,
    String pickupPoint,
    String dropoffPoint,
    Money netRate,
    CurrencyCode sellCurrency,
    BigDecimal fxRate,
    BigDecimal bufferPercent,
    BigDecimal commissionPercent
) {

    public AddTransferLineItemCommand {
        Objects.requireNonNull(supplierId, "supplierId must not be null");
        Objects.requireNonNull(supplierRateId, "supplierRateId must not be null");
        Objects.requireNonNull(vehicleType, "vehicleType must not be null");
        Objects.requireNonNull(pickupPoint, "pickupPoint must not be null");
        Objects.requireNonNull(dropoffPoint, "dropoffPoint must not be null");
        Objects.requireNonNull(netRate, "netRate must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        Objects.requireNonNull(fxRate, "fxRate must not be null");
        Objects.requireNonNull(bufferPercent, "bufferPercent must not be null");
        Objects.requireNonNull(commissionPercent, "commissionPercent must not be null");
    }
}
