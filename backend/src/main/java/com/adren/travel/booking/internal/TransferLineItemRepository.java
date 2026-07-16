package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TransferLineItemRepository extends JpaRepository<TransferLineItem, UUID> {

    List<TransferLineItem> findByItineraryId(UUID itineraryId);
}
