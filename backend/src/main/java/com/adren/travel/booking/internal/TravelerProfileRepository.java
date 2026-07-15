package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TravelerProfileRepository extends JpaRepository<TravelerProfile, UUID> {
}
