package com.adren.travel.payments.internal;

import com.adren.travel.payments.MarkupType;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A Consultant's markup rule for one product category (PRD §12.1, FIN-01)
 * — package-private, own table. Exactly one of {@code percentageValue} or
 * ({@code flatFeeAmount}, {@code flatFeeCurrency}) is populated, matching
 * {@code markupType} — enforced by {@code PaymentsServiceImpl}, not by a
 * DB constraint (RULES.md §4.2 keeps cross-field invariants at the
 * application layer). One row per (consultant, category): configuring a
 * category again replaces its existing rule rather than adding a second
 * one, since "the" 15% hotel markup is a single current value, not a
 * history.
 */
@Entity
@Table(name = "markup_rule", uniqueConstraints = @UniqueConstraint(columnNames = {"consultant_id", "category"}))
class MarkupRule {

    @Id
    private UUID id;

    private UUID consultantId;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    private MarkupType markupType;

    private BigDecimal percentageValue;
    private BigDecimal flatFeeAmount;

    @Enumerated(EnumType.STRING)
    private CurrencyCode flatFeeCurrency;

    private Instant updatedAt;

    protected MarkupRule() {
        // JPA
    }

    MarkupRule(UUID id, UUID consultantId, ProductCategory category, MarkupType markupType,
               BigDecimal percentageValue, BigDecimal flatFeeAmount, CurrencyCode flatFeeCurrency) {
        this.id = id;
        this.consultantId = consultantId;
        this.category = category;
        this.markupType = markupType;
        this.percentageValue = percentageValue;
        this.flatFeeAmount = flatFeeAmount;
        this.flatFeeCurrency = flatFeeCurrency;
        this.updatedAt = Instant.now();
    }

    void update(MarkupType markupType, BigDecimal percentageValue, BigDecimal flatFeeAmount, CurrencyCode flatFeeCurrency) {
        this.markupType = markupType;
        this.percentageValue = percentageValue;
        this.flatFeeAmount = flatFeeAmount;
        this.flatFeeCurrency = flatFeeCurrency;
        this.updatedAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    ProductCategory getCategory() {
        return category;
    }

    MarkupType getMarkupType() {
        return markupType;
    }

    BigDecimal getPercentageValue() {
        return percentageValue;
    }

    BigDecimal getFlatFeeAmount() {
        return flatFeeAmount;
    }

    CurrencyCode getFlatFeeCurrency() {
        return flatFeeCurrency;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
