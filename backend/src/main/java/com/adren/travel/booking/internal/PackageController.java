package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
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

    @GetMapping
    Page<PackageView> findPublished(@RequestParam UUID consultantId, Pageable pageable) {
        return bookingApi.findPublishedPackagesByConsultant(consultantId, pageable);
    }
}
