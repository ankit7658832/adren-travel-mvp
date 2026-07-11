package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
