package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByPnrSearchableRef(String pnrSearchableRef);

    boolean existsByPnrSearchableRef(String pnrSearchableRef);
}
