package com.adren.travel.booking.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TravelPackageRepository extends JpaRepository<TravelPackage, UUID> {

    Page<TravelPackage> findByConsultantIdAndStatus(UUID consultantId, PackageStatus status, Pageable pageable);
}
