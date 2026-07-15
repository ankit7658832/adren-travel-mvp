package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Adds a Hotel line item to an itinerary (PRD §20.2, §9.3, BOK-03).
 * {@code cancellationDeadline} is expected to already be the earliest
 * {@code from} date across the supplier's cancellation policy windows
 * (PRD §10.2.1's Hotelbeds mapping table: "take earliest {@code from} date
 * as the deadline") — that reduction is the supplier adapter's
 * responsibility, not this command's; this story only stores whatever
 * single deadline it's given. {@code sellCurrency}/{@code fxRate}/
 * {@code bufferPercent}/{@code commissionPercent} feed
 * {@code PaymentsApi.calculateSellRate} (FIN-05) to derive the pricing
 * fields the entity stores.
 */
public record AddHotelLineItemCommand(
    SupplierId supplierId,
    String supplierRateId,
    String propertyName,
    String roomType,
    MealPlan mealPlan,
    Instant cancellationDeadline,
    Money netRate,
    CurrencyCode sellCurrency,
    BigDecimal fxRate,
    BigDecimal bufferPercent,
    BigDecimal commissionPercent
) {

    public AddHotelLineItemCommand {
        Objects.requireNonNull(supplierId, "supplierId must not be null");
        Objects.requireNonNull(supplierRateId, "supplierRateId must not be null");
        Objects.requireNonNull(propertyName, "propertyName must not be null");
        Objects.requireNonNull(roomType, "roomType must not be null");
        Objects.requireNonNull(mealPlan, "mealPlan must not be null");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline must not be null");
        Objects.requireNonNull(netRate, "netRate must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        Objects.requireNonNull(fxRate, "fxRate must not be null");
        Objects.requireNonNull(bufferPercent, "bufferPercent must not be null");
        Objects.requireNonNull(commissionPercent, "commissionPercent must not be null");
    }
}
