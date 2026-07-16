package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.CalculateCancellationRefundCommand;
import com.adren.travel.payments.RefundCalculation;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * HTTP surface for the {@code /bookings} resource (PRD §9.1 Flow C, §21.4,
 * BOK-13) — separate from {@link ItineraryController} since it's a
 * different resource ({@code /bookings}, not {@code /itineraries}), per
 * RULES.md §3.1's resource-oriented URL convention.
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

    @PostMapping
    Map<String, UUID> confirm(@Valid @RequestBody ConfirmBookingRequest request) {
        UUID bookingId = bookingApi.confirmBooking(request.quotationOrPackageId(),
            new Money(request.totalSellPrice(), request.currency()));
        return Map.of("bookingId", bookingId);
    }

    /** PRD §21.4's third payment-method option — On-Account billing (FIN-12). */
    @PostMapping("/on-account")
    Map<String, UUID> confirmOnAccount(@Valid @RequestBody ConfirmBookingRequest request) {
        UUID bookingId = bookingApi.confirmBookingOnAccount(request.quotationOrPackageId(),
            new Money(request.totalSellPrice(), request.currency()));
        return Map.of("bookingId", bookingId);
    }

    /** PRD §12.4/§12.5 — calculates (does not process) a cancellation's refund/penalty split (FIN-13). */
    @PostMapping("/{bookingId}/cancellation")
    RefundCalculation calculateCancellationRefund(@PathVariable UUID bookingId,
                                                   @Valid @RequestBody CalculateCancellationRefundRequest request) {
        return bookingApi.calculateCancellationRefund(bookingId, new CalculateCancellationRefundCommand(
            new Money(request.sellPrice(), request.currency()), request.cancellationDeadline(),
            request.cancelledAt(), request.postDeadlinePenaltyPercent()));
    }
}
