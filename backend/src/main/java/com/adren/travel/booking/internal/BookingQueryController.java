package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-side HTTP surface for the Booking module — separate from
 * {@link ItineraryController} since it exposes a different resource
 * ({@code /bookings}, not {@code /itineraries}), per RULES.md §3.1's
 * resource-oriented URL convention.
 */
@RestController
@RequestMapping("/api/v1/bookings")
class BookingQueryController {

    private final BookingApi bookingApi;

    BookingQueryController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @GetMapping
    PageResponse<UUID> findByConsultant(@RequestParam UUID consultantId, Pageable pageable) {
        return PageResponse.of(bookingApi.findBookingsByConsultant(consultantId, pageable));
    }
}
