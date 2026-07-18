package com.adren.travel.supplier.internal.localdmc;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One manually-catalogued Local DMC product (PRD §10.2.8, DMC-03/10/11) —
 * package-private, own table. {@code updatedAt} is what DMC-11's scheduled
 * staleness check compares against a threshold.
 */
@Entity
@Table(name = "local_dmc_inventory_item")
public class LocalDmcInventoryItem {

    @Id
    private UUID itemId;

    private UUID localDmcId;
    private String productName;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    private BigDecimal netRate;

    @Enumerated(EnumType.STRING)
    private CurrencyCode netRateCurrency;

    private String cancellationPolicyText;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private Instant createdAt;
    private Instant updatedAt;

    protected LocalDmcInventoryItem() {
        // JPA
    }

    public LocalDmcInventoryItem(UUID itemId, UUID localDmcId, String productName, ProductCategory category,
                                  BigDecimal netRate, CurrencyCode netRateCurrency, String cancellationPolicyText,
                                  LocalDate availableFrom, LocalDate availableTo) {
        this.itemId = itemId;
        this.localDmcId = localDmcId;
        this.productName = productName;
        this.category = category;
        this.netRate = netRate;
        this.netRateCurrency = netRateCurrency;
        this.cancellationPolicyText = cancellationPolicyText;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** DMC-10: edits are immediately reflected in subsequent search results — the rate change AC's actual mechanism. */
    public void update(String productName, ProductCategory category, BigDecimal netRate, CurrencyCode netRateCurrency,
                        String cancellationPolicyText, LocalDate availableFrom, LocalDate availableTo) {
        this.productName = productName;
        this.category = category;
        this.netRate = netRate;
        this.netRateCurrency = netRateCurrency;
        this.cancellationPolicyText = cancellationPolicyText;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
        this.updatedAt = Instant.now();
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getLocalDmcId() {
        return localDmcId;
    }

    public String getProductName() {
        return productName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getNetRate() {
        return netRate;
    }

    public CurrencyCode getNetRateCurrency() {
        return netRateCurrency;
    }

    public String getCancellationPolicyText() {
        return cancellationPolicyText;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public LocalDate getAvailableTo() {
        return availableTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
