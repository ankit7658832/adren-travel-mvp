package com.adren.travel.booking.internal;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * PRD §9.1 Flow A, steps 2-4 — multi-location geocoded search, the entry
 * point the Search Dashboard (21.1) drives (FND-13).
 */
@RestController
@RequestMapping("/api/v1/search")
class SearchController {

    private final GeocodeAndSearchService geocodeAndSearchService;

    SearchController(GeocodeAndSearchService geocodeAndSearchService) {
        this.geocodeAndSearchService = geocodeAndSearchService;
    }

    @PostMapping
    SearchResponse search(@Valid @RequestBody SearchRequest request) {
        LocalDate checkIn = request.checkIn() != null ? request.checkIn() : LocalDate.now().plusDays(30);
        LocalDate checkOut = request.checkOut() != null ? request.checkOut() : checkIn.plusDays(3);
        return new SearchResponse(geocodeAndSearchService.geocodeAndSearch(request.locationQueries(), checkIn, checkOut));
    }
}
