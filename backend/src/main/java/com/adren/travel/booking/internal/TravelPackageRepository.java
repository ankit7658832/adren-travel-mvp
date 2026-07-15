package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TravelPackageRepository extends JpaRepository<TravelPackage, UUID> {
}
