package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Adds a Flight line item to an itinerary (PRD §20.3, §10.2.4, BOK-04).
 * {@code supplierRateId} maps to Mystifly's {@code FareBasisCode} (§10.2.4's
 * field-mapping table); unlike {@link AddHotelLineItemCommand}, there is no
 * {@code cancellationDeadline}-equivalent field — flight fares expire on
 * their own faster clock (§10.2.4: "typically expire in minutes, not
 * hours"), which AI-09's stale-price re-validation handles at booking time,
 * not at line-item-add time. {@code sellCurrency}/{@code fxRate}/
 * {@code bufferPercent}/{@code commissionPercent} feed {@code
 * PaymentsApi.calculateSellRate} (FIN-05) the same way the hotel command does.
 */
public record AddFlightLineItemCommand(
    SupplierId supplierId,
    String supplierRateId,
    String airlineCode,
    String flightNumber,
    CabinClass cabinClass,
    String baggageAllowance,
    Money netRate,
    CurrencyCode sellCurrency,
    BigDecimal fxRate,
    BigDecimal bufferPercent,
    BigDecimal commissionPercent
) {

    public AddFlightLineItemCommand {
        Objects.requireNonNull(supplierId, "supplierId must not be null");
        Objects.requireNonNull(supplierRateId, "supplierRateId must not be null");
        Objects.requireNonNull(airlineCode, "airlineCode must not be null");
        Objects.requireNonNull(flightNumber, "flightNumber must not be null");
        Objects.requireNonNull(cabinClass, "cabinClass must not be null");
        Objects.requireNonNull(netRate, "netRate must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        Objects.requireNonNull(fxRate, "fxRate must not be null");
        Objects.requireNonNull(bufferPercent, "bufferPercent must not be null");
        Objects.requireNonNull(commissionPercent, "commissionPercent must not be null");
    }
}
