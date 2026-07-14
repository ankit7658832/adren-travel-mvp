package com.adren.travel.booking.internal;

import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.BookingApi;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * HTTP surface for the Booking module (consumed by the React+Vite frontend).
 * Controllers stay thin — all logic lives in {@link BookingServiceImpl},
 * reached here only through the public {@link BookingApi}.
 */
@RestController
@RequestMapping("/api/v1/itineraries")
class ItineraryController {

    private final BookingApi bookingApi;

    ItineraryController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @PostMapping("/{itineraryId}/quotation")
    UUID saveAsQuotation(@PathVariable UUID itineraryId) {
        return bookingApi.saveAsQuotation(itineraryId);
    }

    /**
     * The Itinerary Builder's alternate-selection side panel (PRD §21.2,
     * FND-16) — {@code checkIn}/{@code checkOut} default the same way
     * {@code SearchController} does when the Consultant hasn't picked
     * dates yet.
     */
    @GetMapping("/{itineraryId}/alternates")
    List<AlternateOption> findAlternates(
        @PathVariable UUID itineraryId,
        @RequestParam String location,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        LocalDate resolvedCheckIn = checkIn != null ? checkIn : LocalDate.now().plusDays(30);
        LocalDate resolvedCheckOut = checkOut != null ? checkOut : resolvedCheckIn.plusDays(3);
        return bookingApi.findAlternates(itineraryId, location, category, resolvedCheckIn, resolvedCheckOut);
    }
}
