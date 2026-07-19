package com.adren.travel.notification.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * HRD-03 / RULES.md §2.2 — a claimed {@code (eventId, listenerName)} pair.
 * The unique constraint on those two columns (V38) IS the idempotency
 * guarantee: {@link ProcessedEventDeduplicationService#tryClaim} inserts
 * this row before a listener dispatches, and a redelivery of the same
 * event to the same listener loses the DB-level race, not an in-memory
 * cache (which wouldn't survive the crash-and-retry this guards against).
 */
@Entity
@Table(name = "processed_event", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "listener_name"}))
class ProcessedEvent {

    @Id
    private UUID id;

    private String eventId;
    private String listenerName;
    private Instant processedAt;

    protected ProcessedEvent() {
        // JPA
    }

    ProcessedEvent(String eventId, String listenerName) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.listenerName = listenerName;
        this.processedAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    String getEventId() {
        return eventId;
    }

    String getListenerName() {
        return listenerName;
    }

    Instant getProcessedAt() {
        return processedAt;
    }
}
