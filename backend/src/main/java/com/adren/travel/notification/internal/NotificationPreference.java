package com.adren.travel.notification.internal;

import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * HRD-04 — a Consultant's explicit override of HRD-01's regional secondary
 * channel default (PRD §21.10). No row for a Consultant means no override
 * — {@link NotificationDispatcher} falls back to {@link SecondaryChannelProvider}'s
 * market default, exactly as it always has.
 */
@Entity
@Table(name = "notification_preference")
class NotificationPreference {

    @Id
    private UUID consultantId;

    @Enumerated(EnumType.STRING)
    private NotificationChannel secondaryChannel;

    private Instant updatedAt;

    protected NotificationPreference() {
        // JPA
    }

    NotificationPreference(UUID consultantId, NotificationChannel secondaryChannel) {
        this.consultantId = consultantId;
        this.secondaryChannel = secondaryChannel;
        this.updatedAt = Instant.now();
    }

    void override(NotificationChannel secondaryChannel) {
        this.secondaryChannel = secondaryChannel;
        this.updatedAt = Instant.now();
    }

    UUID getConsultantId() {
        return consultantId;
    }

    NotificationChannel getSecondaryChannel() {
        return secondaryChannel;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
