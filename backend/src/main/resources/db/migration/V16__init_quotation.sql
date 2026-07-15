-- Booking module: Quotation (PRD Section 20.9, BOK-09). Real FK to
-- itinerary since both are booking-owned (RULES.md Section 4.2's
-- "value not constraint" rule is about cross-module references only).

CREATE TABLE quotation (
    quotation_id           UUID PRIMARY KEY,
    itinerary_id           UUID NOT NULL REFERENCES itinerary(itinerary_id),
    valid_until            TIMESTAMP WITH TIME ZONE NOT NULL,
    shared_with_traveler   BOOLEAN NOT NULL DEFAULT FALSE,
    converted_to_booking_id UUID,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL
);
