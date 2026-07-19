package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.BookingSearchResultView;
import com.adren.travel.booking.CalculateCancellationRefundCommand;
import com.adren.travel.booking.CancellationRequestView;
import com.adren.travel.booking.DisputeTicketView;
import com.adren.travel.booking.FlagDisputeCommand;
import com.adren.travel.payments.FxRateSnapshot;
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

    /** PRD §16, §22.8 T12, HRD-07 — PNR/booking reference search across all product types. */
    @GetMapping("/search")
    PageResponse<BookingSearchResultView> search(@RequestParam String ref, Pageable pageable) {
        return PageResponse.of(bookingApi.searchByPnrReference(ref, pageable));
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
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(request.originalSupplierCurrency(),
            request.currency(), request.originalFxRate(), request.originalFxSnapshotAt());
        return bookingApi.calculateCancellationRefund(bookingId, new CalculateCancellationRefundCommand(
            new Money(request.sellPrice(), request.currency()), request.cancellationDeadline(),
            request.cancelledAt(), request.postDeadlinePenaltyPercent(), originalFxRateSnapshot));
    }

    /**
     * PRD §12.5, FIN-16 — submits a cancellation, starting the tracked
     * policy-check → refund-calculation → approval-if-penalized workflow.
     * Unlike {@link #calculateCancellationRefund} (a pure preview), this
     * persists a {@code CancellationRequest} and, for a penalty-free
     * cancellation, processes the refund immediately.
     */
    @PostMapping("/{bookingId}/cancellation-requests")
    CancellationRequestView submitCancellation(@PathVariable UUID bookingId,
                                                @Valid @RequestBody CalculateCancellationRefundRequest request) {
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(request.originalSupplierCurrency(),
            request.currency(), request.originalFxRate(), request.originalFxSnapshotAt());
        return bookingApi.submitCancellation(bookingId, new CalculateCancellationRefundCommand(
            new Money(request.sellPrice(), request.currency()), request.cancellationDeadline(),
            request.cancelledAt(), request.postDeadlinePenaltyPercent(), originalFxRateSnapshot));
    }

    /** PRD §12.5, FIN-16 — a Consultant's explicit sign-off on a penalized cancellation, processing its refund. */
    @PostMapping("/cancellation-requests/{cancellationRequestId}/approval")
    CancellationRequestView approveCancellation(@PathVariable UUID cancellationRequestId) {
        return bookingApi.approveCancellation(cancellationRequestId);
    }

    /** PRD §12.5, FIN-16 — flags a dispute on a booking, creating a tracked ticket rather than an email handoff. */
    @PostMapping("/{bookingId}/disputes")
    DisputeTicketView flagDispute(@PathVariable UUID bookingId, @Valid @RequestBody FlagDisputeRequest request) {
        return bookingApi.flagDispute(bookingId, new FlagDisputeCommand(request.reason()));
    }
}
