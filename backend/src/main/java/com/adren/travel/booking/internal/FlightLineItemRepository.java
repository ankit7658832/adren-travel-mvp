package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FlightLineItemRepository extends JpaRepository<FlightLineItem, UUID> {

    List<FlightLineItem> findByItineraryId(UUID itineraryId);
}
