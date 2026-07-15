package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface HotelLineItemRepository extends JpaRepository<HotelLineItem, UUID> {

    List<HotelLineItem> findByItineraryId(UUID itineraryId);
}
