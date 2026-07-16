package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.ConvertQuotationToPackageCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** PRD §9.1 Flow B, §20.7 — converts a Quotation into a Package (BOK-10). */
@RestController
@RequestMapping("/api/v1/quotations")
class QuotationController {

    private final BookingApi bookingApi;

    QuotationController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @PostMapping("/{quotationId}/package")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> convertToPackage(@PathVariable UUID quotationId,
                                        @Valid @RequestBody ConvertQuotationToPackageRequest request) {
        UUID packageId = bookingApi.convertQuotationToPackage(quotationId, new ConvertQuotationToPackageCommand(
            request.name(), request.description(), request.validityStart(), request.validityEnd(),
            request.markupPrice(), request.maxPax()));
        return Map.of("packageId", packageId);
    }

    /** PRD §23.1 Edge Case #3 — recalculates a Quotation on traveler-count change (BOK-18). */
    @PatchMapping("/{quotationId}/traveler-count")
    void recalculate(@PathVariable UUID quotationId, @Valid @RequestBody RecalculateQuotationRequest request) {
        bookingApi.recalculateQuotation(quotationId, request.travelerCount());
    }
}
