package com.adren.travel.booking.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * First request body this codebase has had (RULES.md §3.5) — validated via
 * {@code @Valid}/Bean Validation from the start rather than letting the
 * "we haven't needed it yet" precedent (true only because
 * {@code ItineraryController} had nothing but a path variable) become "we
 * don't validate here either."
 */
record SearchRequest(
    @NotEmpty(message = "at least one location is required") List<@NotBlank String> locationQueries,
    LocalDate checkIn,
    LocalDate checkOut
) {
}
