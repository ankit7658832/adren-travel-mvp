package com.adren.travel.booking.internal;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Traveler Profile entity per PRD §20.10 — package-private, package-owned
 * table (RULES.md §4.2). {@code passportNumber}/{@code passportExpiry}/
 * {@code nationality} are nullable at the data-model level: PRD §20.10
 * names them "required for cruise/international bookings", but that
 * conditional gate lives at booking-confirmation time against a specific
 * line item's {@code passengerDocumentsRequired} flag — no cruise line
 * item type exists yet in this Hotelbeds-only vertical slice (BOK-06 is
 * out of scope), so the gate itself isn't wired here; this entity is
 * ready for it once a cruise line item type exists to check against.
 * {@code documentVaultReferences} are opaque storage-reference strings
 * (e.g. an S3 key) — the referenced files' own encryption-at-rest is a
 * separate concern (S3 SSE-KMS), not modeled by this entity.
 */
@Entity
@Table(name = "traveler_profile")
class TravelerProfile {

    @Id
    private UUID travelerId;

    private UUID consultantId;
    private String name;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private LocalDate passportExpiry;
    private String nationality;

    @ElementCollection
    @CollectionTable(name = "traveler_profile_document", joinColumns = @JoinColumn(name = "traveler_id"))
    @Column(name = "document_reference")
    private List<String> documentVaultReferences;

    @ElementCollection
    @CollectionTable(name = "traveler_profile_preference", joinColumns = @JoinColumn(name = "traveler_id"))
    @MapKeyColumn(name = "preference_key")
    @Column(name = "preference_value")
    private Map<String, String> preferences;

    private Instant createdAt;
    private Instant updatedAt;

    protected TravelerProfile() {
        // JPA
    }

    TravelerProfile(UUID travelerId, UUID consultantId, String name, LocalDate dateOfBirth, String passportNumber,
                     LocalDate passportExpiry, String nationality, List<String> documentVaultReferences,
                     Map<String, String> preferences) {
        this.travelerId = travelerId;
        this.consultantId = consultantId;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.passportNumber = passportNumber;
        this.passportExpiry = passportExpiry;
        this.nationality = nationality;
        this.documentVaultReferences = documentVaultReferences;
        this.preferences = preferences;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    UUID getTravelerId() {
        return travelerId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getName() {
        return name;
    }

    LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    String getPassportNumber() {
        return passportNumber;
    }

    LocalDate getPassportExpiry() {
        return passportExpiry;
    }

    String getNationality() {
        return nationality;
    }

    List<String> getDocumentVaultReferences() {
        return documentVaultReferences;
    }

    Map<String, String> getPreferences() {
        return preferences;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
