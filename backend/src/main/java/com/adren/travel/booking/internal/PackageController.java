package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** PRD §9.1 Flow B step 3, §20.7, §22.3 — publishing and browsing Packages (BOK-12). */
@RestController
@RequestMapping("/api/v1/packages")
class PackageController {

    private final BookingApi bookingApi;

    PackageController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @PostMapping("/{packageId}/publish")
    UUID publish(@PathVariable UUID packageId, @RequestBody PublishPackageRequest request) {
        return bookingApi.publishPackage(packageId, request.promoteViaAds());
    }

    /** PRD §17.2/§22.3 T5 — completes the UK ATOL disclosure step, a precondition for publishing a dynamic combo (BOK-11). */
    @PostMapping("/{packageId}/atol-disclosure")
    void completeAtolDisclosure(@PathVariable UUID packageId) {
        bookingApi.completeAtolDisclosure(packageId);
    }

    /** PRD §23.5 Edge Case #11, ADS-12 — editing a Package's price; {@code ads} reacts by auto-pausing any Live campaign promoting it. */
    @PostMapping("/{packageId}/price")
    void updatePrice(@PathVariable UUID packageId, @Valid @RequestBody UpdatePackagePriceRequest request) {
        bookingApi.updatePackagePrice(packageId, request.markupPrice());
    }

    @GetMapping
    Page<PackageView> findPublished(@RequestParam UUID consultantId, Pageable pageable) {
        return bookingApi.findPublishedPackagesByConsultant(consultantId, pageable);
    }
}
