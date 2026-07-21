package com.adren.travel.booking.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CruiseLineItemRepository extends JpaRepository<CruiseLineItem, UUID> {

    List<CruiseLineItem> findByItineraryId(UUID itineraryId);

    /** HRD-11 — the Super Admin Dashboard's per-supplier performance summary. */
    long countBySupplierId(SupplierId supplierId);
}
