package com.adren.travel.booking.internal;

import com.adren.travel.booking.CabinClass;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.supplier.SupplierId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A Flight line item on an Itinerary (PRD §20.3, §10.2.4, BOK-04) —
 * package-private, own table. Same pricing-field shape as {@link
 * HotelLineItem} (net rate keeps the supplier's currency, markup/buffer/
 * sell rate share {@code sellCurrency}, {@code fxRateSnapshot} is the locked
 * rate value) — see {@code HotelLineItem}'s Javadoc for why.
 */
@Entity
@Table(name = "flight_line_item")
class FlightLineItem {

    @Id
    private UUID lineItemId;

    private UUID itineraryId;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private String supplierRateId;
    private String airlineCode;
    private String flightNumber;

    @Enumerated(EnumType.STRING)
    private CabinClass cabinClass;

    private String baggageAllowance;

    private BigDecimal netRate;

    @Enumerated(EnumType.STRING)
    private CurrencyCode netRateCurrency;

    private BigDecimal markupApplied;
    private BigDecimal currencyBufferApplied;
    private BigDecimal sellRate;

    @Enumerated(EnumType.STRING)
    private CurrencyCode sellCurrency;

    private BigDecimal fxRateSnapshot;

    private Instant createdAt;

    protected FlightLineItem() {
        // JPA
    }

    FlightLineItem(UUID lineItemId, UUID itineraryId, SupplierId supplierId, String supplierRateId,
                   String airlineCode, String flightNumber, CabinClass cabinClass, String baggageAllowance,
                   BigDecimal netRate, CurrencyCode netRateCurrency, BigDecimal markupApplied,
                   BigDecimal currencyBufferApplied, BigDecimal sellRate, CurrencyCode sellCurrency,
                   BigDecimal fxRateSnapshot) {
        this.lineItemId = lineItemId;
        this.itineraryId = itineraryId;
        this.supplierId = supplierId;
        this.supplierRateId = supplierRateId;
        this.airlineCode = airlineCode;
        this.flightNumber = flightNumber;
        this.cabinClass = cabinClass;
        this.baggageAllowance = baggageAllowance;
        this.netRate = netRate;
        this.netRateCurrency = netRateCurrency;
        this.markupApplied = markupApplied;
        this.currencyBufferApplied = currencyBufferApplied;
        this.sellRate = sellRate;
        this.sellCurrency = sellCurrency;
        this.fxRateSnapshot = fxRateSnapshot;
        this.createdAt = Instant.now();
    }

    UUID getLineItemId() {
        return lineItemId;
    }

    UUID getItineraryId() {
        return itineraryId;
    }

    String getAirlineCode() {
        return airlineCode;
    }

    String getFlightNumber() {
        return flightNumber;
    }

    CabinClass getCabinClass() {
        return cabinClass;
    }

    String getBaggageAllowance() {
        return baggageAllowance;
    }

    BigDecimal getSellRate() {
        return sellRate;
    }

    CurrencyCode getSellCurrency() {
        return sellCurrency;
    }

    BigDecimal getMarkupApplied() {
        return markupApplied;
    }

    BigDecimal getCurrencyBufferApplied() {
        return currencyBufferApplied;
    }

    BigDecimal getFxRateSnapshot() {
        return fxRateSnapshot;
    }
}
