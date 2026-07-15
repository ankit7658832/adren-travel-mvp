package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.CreateTravelerProfileCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * PRD §20.10 — Traveler Profile capture (BOK-14). Controller depends on
 * {@link BookingApi} only (RULES.md §1.2).
 */
@RestController
@RequestMapping("/api/v1/travelers")
class TravelerController {

    private final BookingApi bookingApi;

    TravelerController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> create(@Valid @RequestBody CreateTravelerProfileRequest request) {
        UUID travelerId = bookingApi.createTravelerProfile(new CreateTravelerProfileCommand(
            request.name(), request.dateOfBirth(), request.passportNumber(), request.passportExpiry(),
            request.nationality(), request.documentVaultReferences(), request.preferences()));
        return Map.of("travelerId", travelerId);
    }
}
