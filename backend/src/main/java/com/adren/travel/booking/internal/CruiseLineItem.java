package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.supplier.SupplierId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Cruise line item on an Itinerary (PRD §20.5, §10.2.6, BOK-06) —
 * package-private, own table. Same pricing-field shape as {@link
 * HotelLineItem}; see its Javadoc for why. {@code ports} is Widgety's own
 * multi-port itinerary structure flattened into metadata on this single line
 * item (§10.2.6), stored as an {@code @ElementCollection} the same way
 * {@code TravelerProfile.documentVaultReferences} already does.
 */
@Entity
@Table(name = "cruise_line_item")
class CruiseLineItem {

    @Id
    private UUID lineItemId;

    private UUID itineraryId;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private String supplierRateId;
    private String cruiseLine;
    private String cabinCategory;

    @ElementCollection
    @CollectionTable(name = "cruise_line_item_port", joinColumns = @JoinColumn(name = "line_item_id"))
    @Column(name = "port")
    private List<String> ports = new ArrayList<>();

    private boolean passengerDocumentsRequired;

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

    protected CruiseLineItem() {
        // JPA
    }

    CruiseLineItem(UUID lineItemId, UUID itineraryId, SupplierId supplierId, String supplierRateId,
                   String cruiseLine, String cabinCategory, List<String> ports, boolean passengerDocumentsRequired,
                   BigDecimal netRate, CurrencyCode netRateCurrency, BigDecimal markupApplied,
                   BigDecimal currencyBufferApplied, BigDecimal sellRate, CurrencyCode sellCurrency,
                   BigDecimal fxRateSnapshot) {
        this.lineItemId = lineItemId;
        this.itineraryId = itineraryId;
        this.supplierId = supplierId;
        this.supplierRateId = supplierRateId;
        this.cruiseLine = cruiseLine;
        this.cabinCategory = cabinCategory;
        this.ports = new ArrayList<>(ports);
        this.passengerDocumentsRequired = passengerDocumentsRequired;
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

    String getCruiseLine() {
        return cruiseLine;
    }

    String getCabinCategory() {
        return cabinCategory;
    }

    List<String> getPorts() {
        return ports;
    }

    boolean isPassengerDocumentsRequired() {
        return passengerDocumentsRequired;
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
