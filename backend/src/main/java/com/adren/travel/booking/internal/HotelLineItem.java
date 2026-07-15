package com.adren.travel.booking.internal;

import com.adren.travel.booking.MealPlan;
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
 * A Hotel line item on an Itinerary (PRD §20.2, §9.3, BOK-03) —
 * package-private, own table. {@code itineraryId} is a real FK constraint
 * (see the migration) since {@code Itinerary} is owned by this SAME
 * module — RULES.md §4.2's "value not constraint" rule is about
 * CROSS-module references, not intra-module ones. {@code netRate} keeps
 * its own currency (the supplier's), while {@code markupApplied},
 * {@code currencyBufferApplied} and {@code sellRate} share
 * {@code sellCurrency} — the output of {@code PaymentsApi.calculateSellRate}
 * (FIN-05). {@code fxRateSnapshot} is the locked rate value itself (PRD
 * §20.2: "Decimal — locked at quote time"), not the full snapshot object.
 */
@Entity
@Table(name = "hotel_line_item")
class HotelLineItem {

    @Id
    private UUID lineItemId;

    private UUID itineraryId;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private String supplierRateId;
    private String propertyName;
    private String roomType;

    @Enumerated(EnumType.STRING)
    private MealPlan mealPlan;

    private Instant cancellationDeadline;

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

    protected HotelLineItem() {
        // JPA
    }

    HotelLineItem(UUID lineItemId, UUID itineraryId, SupplierId supplierId, String supplierRateId,
                  String propertyName, String roomType, MealPlan mealPlan, Instant cancellationDeadline,
                  BigDecimal netRate, CurrencyCode netRateCurrency, BigDecimal markupApplied,
                  BigDecimal currencyBufferApplied, BigDecimal sellRate, CurrencyCode sellCurrency,
                  BigDecimal fxRateSnapshot) {
        this.lineItemId = lineItemId;
        this.itineraryId = itineraryId;
        this.supplierId = supplierId;
        this.supplierRateId = supplierRateId;
        this.propertyName = propertyName;
        this.roomType = roomType;
        this.mealPlan = mealPlan;
        this.cancellationDeadline = cancellationDeadline;
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

    String getSupplierRateId() {
        return supplierRateId;
    }

    String getPropertyName() {
        return propertyName;
    }

    String getRoomType() {
        return roomType;
    }

    MealPlan getMealPlan() {
        return mealPlan;
    }

    Instant getCancellationDeadline() {
        return cancellationDeadline;
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
