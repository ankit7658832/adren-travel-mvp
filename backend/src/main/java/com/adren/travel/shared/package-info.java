/**
 * Shared kernel: value objects with no business logic of their own, safe to
 * depend on from every other module (Money, CurrencyCode, etc.).
 * <p>
 * This module is declared {@code OPEN} — unlike other modules, all of its
 * sub-packages are directly accessible, not just types in the root package.
 * Keep it small and stable; if a type here starts accumulating business
 * rules, it belongs in a real module instead.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared Kernel",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.adren.travel.shared;
