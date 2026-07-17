package com.adren.travel.booking.internal;

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
 * A Transfer line item on an Itinerary (PRD §20.4, §10.2.5, BOK-05) —
 * package-private, own table. Same pricing-field shape as {@link
 * HotelLineItem}; see its Javadoc for why.
 */
@Entity
@Table(name = "transfer_line_item")
class TransferLineItem {

    @Id
    private UUID lineItemId;

    private UUID itineraryId;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private String supplierRateId;
    private String vehicleType;
    private String pickupPoint;
    private String dropoffPoint;

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

    protected TransferLineItem() {
        // JPA
    }

    TransferLineItem(UUID lineItemId, UUID itineraryId, SupplierId supplierId, String supplierRateId,
                      String vehicleType, String pickupPoint, String dropoffPoint, BigDecimal netRate,
                      CurrencyCode netRateCurrency, BigDecimal markupApplied, BigDecimal currencyBufferApplied,
                      BigDecimal sellRate, CurrencyCode sellCurrency, BigDecimal fxRateSnapshot) {
        this.lineItemId = lineItemId;
        this.itineraryId = itineraryId;
        this.supplierId = supplierId;
        this.supplierRateId = supplierRateId;
        this.vehicleType = vehicleType;
        this.pickupPoint = pickupPoint;
        this.dropoffPoint = dropoffPoint;
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

    String getVehicleType() {
        return vehicleType;
    }

    String getPickupPoint() {
        return pickupPoint;
    }

    String getDropoffPoint() {
        return dropoffPoint;
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
