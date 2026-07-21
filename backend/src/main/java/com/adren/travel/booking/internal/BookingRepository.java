package com.adren.travel.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByPnrSearchableRef(String pnrSearchableRef);

    boolean existsByPnrSearchableRef(String pnrSearchableRef);

    /** HRD-09 — the Consultant Dashboard's top-packages ranking, all-time (not month-bounded). */
    List<Booking> findByConsultantId(UUID consultantId);

    /** HRD-09 — "bookings this month" and "GMV this month" summary cards, the same filtered set backing both. */
    List<Booking> findByConsultantIdAndCreatedAtGreaterThanEqual(UUID consultantId, Instant monthStart);

    /**
     * HRD-11 — all-Consultant GMV, grouped by settlement currency (RULES.md
     * §4.4: summing across currencies is invalid, so this can never
     * collapse to a single total). Platform scope — no consultantId filter.
     */
    @Query("SELECT b.totalSellCurrency, SUM(b.totalSellPriceAmount) FROM Booking b GROUP BY b.totalSellCurrency")
    List<Object[]> sumTotalSellPriceGroupedByCurrency();
}
