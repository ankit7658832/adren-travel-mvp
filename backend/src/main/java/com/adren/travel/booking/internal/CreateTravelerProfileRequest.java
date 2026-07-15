package com.adren.travel.booking.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

record CreateTravelerProfileRequest(
    @NotBlank String name,
    @NotNull LocalDate dateOfBirth,
    String passportNumber,
    LocalDate passportExpiry,
    String nationality,
    List<String> documentVaultReferences,
    Map<String, String> preferences
) {
}
