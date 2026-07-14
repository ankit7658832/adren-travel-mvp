package com.adren.travel.whitelabel.internal;

import com.adren.travel.shared.LocaleCode;
import com.adren.travel.whitelabel.ConsultantStatus;
import com.adren.travel.whitelabel.Market;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Consultant entity per PRD §13.1/§20 — package-private, package-owned
 * table (RULES.md §4.2). {@code kycFields} is a flexible key/value map
 * (own table, not JSONB) since the field set varies per {@link Market}
 * per the data-driven KYC rule (RULES.md §24.7) rather than a fixed column
 * per possible field.
 */
@Entity
@Table(name = "consultant")
class Consultant {

    @Id
    private UUID consultantId;

    private String businessName;

    @Enumerated(EnumType.STRING)
    private Market homeMarket;

    @Enumerated(EnumType.STRING)
    private ConsultantStatus status;

    // PRD §13.3/FND-17 — English is every market's primary/default
    // language; MarketLocaleProvider is the data-driven catalog of what
    // else a Consultant in this homeMarket may pick as a secondary.
    @Enumerated(EnumType.STRING)
    private LocaleCode preferredLocale;

    @ElementCollection
    @CollectionTable(name = "consultant_kyc_field", joinColumns = @JoinColumn(name = "consultant_id"))
    @MapKeyColumn(name = "field_key")
    @Column(name = "field_value")
    private Map<String, String> kycFields;

    private Instant createdAt;

    protected Consultant() {
        // JPA
    }

    Consultant(UUID consultantId, String businessName, Market homeMarket, Map<String, String> kycFields) {
        this.consultantId = consultantId;
        this.businessName = businessName;
        this.homeMarket = homeMarket;
        this.kycFields = kycFields;
        this.status = ConsultantStatus.ACTIVE;
        this.preferredLocale = LocaleCode.EN;
        this.createdAt = Instant.now();
    }

    void changePreferredLocale(LocaleCode locale) {
        this.preferredLocale = locale;
    }

    void suspend() {
        if (this.status != ConsultantStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE consultant can be suspended, was: " + this.status);
        }
        this.status = ConsultantStatus.SUSPENDED;
    }

    void reinstate() {
        if (this.status != ConsultantStatus.SUSPENDED) {
            throw new IllegalStateException("Only a SUSPENDED consultant can be reinstated, was: " + this.status);
        }
        this.status = ConsultantStatus.ACTIVE;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getBusinessName() {
        return businessName;
    }

    Market getHomeMarket() {
        return homeMarket;
    }

    ConsultantStatus getStatus() {
        return status;
    }

    LocaleCode getPreferredLocale() {
        return preferredLocale;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
