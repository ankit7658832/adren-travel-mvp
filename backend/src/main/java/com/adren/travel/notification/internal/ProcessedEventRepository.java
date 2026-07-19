package com.adren.travel.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndListenerName(String eventId, String listenerName);
}
