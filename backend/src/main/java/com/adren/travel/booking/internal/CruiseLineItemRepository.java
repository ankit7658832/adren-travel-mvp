package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CruiseLineItemRepository extends JpaRepository<CruiseLineItem, UUID> {

    List<CruiseLineItem> findByItineraryId(UUID itineraryId);
}
