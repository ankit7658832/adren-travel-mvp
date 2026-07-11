package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    List<Itinerary> findByConsultantId(UUID consultantId);
}
