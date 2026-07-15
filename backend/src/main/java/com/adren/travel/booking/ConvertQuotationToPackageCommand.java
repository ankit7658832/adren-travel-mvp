package com.adren.travel.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Converts a saved Quotation into a reusable Package (PRD §9.1 Flow B,
 * §20.7, BOK-10). {@code basePrice} is not part of this command — it's
 * auto-filled server-side from the sum of the source itinerary's priced
 * line items (FIN-05's {@code sell_rate}), matching this story's "base
 * auto-filled, markup editable" design note. {@code markupPrice} is the
 * Consultant-editable amount added on top, in the same currency as the
 * auto-filled base (a package-level markup, distinct from the per-line-item
 * markup already baked into each line item's {@code sell_rate}).
 */
public record ConvertQuotationToPackageCommand(
    String name,
    String description,
    LocalDate validityStart,
    LocalDate validityEnd,
    BigDecimal markupPrice,
    Integer maxPax
) {

    public ConvertQuotationToPackageCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(validityStart, "validityStart must not be null");
        Objects.requireNonNull(validityEnd, "validityEnd must not be null");
        Objects.requireNonNull(markupPrice, "markupPrice must not be null");
        Objects.requireNonNull(maxPax, "maxPax must not be null");
    }
}
