package com.adren.travel.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cross-module-safe input to {@link BookingApi#createTravelerProfile}
 * (PRD §20.10, BOK-14) — a plain value, never a JPA entity (RULES.md
 * §1.4). Never carries a consultantId — scoped to the CALLING principal's
 * own tenant, mirroring {@code whitelabel.AddUserCommand}.
 */
public record CreateTravelerProfileCommand(
    String name,
    LocalDate dateOfBirth,
    String passportNumber,
    LocalDate passportExpiry,
    String nationality,
    List<String> documentVaultReferences,
    Map<String, String> preferences
) {
}
