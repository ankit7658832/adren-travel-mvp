package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.DisputeTicketView;
import com.adren.travel.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP surface for HRD-06's dispute-ticket tracker — separate from {@link
 * BookingQueryController} since {@code /disputes} is its own resource
 * spanning potentially many bookings, not nested under one {@code
 * /bookings/{bookingId}} (RULES.md §3.1's resource-oriented URL
 * convention, same reasoning {@code BookingQueryController}'s own Javadoc
 * gives for not merging with {@code ItineraryController}).
 */
@RestController
@RequestMapping("/api/v1/disputes")
class DisputeController {

    private final BookingApi bookingApi;

    DisputeController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @GetMapping
    PageResponse<DisputeTicketView> findAll(@RequestParam(required = false) UUID consultantId, Pageable pageable) {
        return PageResponse.of(bookingApi.findDisputeTickets(consultantId, pageable));
    }
}
