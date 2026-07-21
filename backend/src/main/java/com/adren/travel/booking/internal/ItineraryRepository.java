package com.adren.travel.booking.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    Page<Itinerary> findByConsultantId(UUID consultantId, Pageable pageable);

    /** HRD-09 — the Consultant Dashboard's Pending Quotations tab: an itinerary sitting at QUOTATION hasn't yet converted to a booking. */
    Page<Itinerary> findByConsultantIdAndStatus(UUID consultantId, ItineraryStatus status, Pageable pageable);
}
