-- Booking module: Hotel line item (PRD Section 20.2, Section 9.3 — BOK-03).
-- itinerary_id is a real FK constraint since Itinerary is owned by this
-- SAME module (RULES.md Section 4.2's "value not constraint" rule is
-- about cross-module references only).

CREATE TABLE hotel_line_item (
    line_item_id             UUID PRIMARY KEY,
    itinerary_id             UUID NOT NULL REFERENCES itinerary(itinerary_id),
    supplier_id              VARCHAR(20) NOT NULL,
    supplier_rate_id         VARCHAR(255) NOT NULL,
    property_name            VARCHAR(255) NOT NULL,
    room_type                VARCHAR(255) NOT NULL,
    meal_plan                VARCHAR(10) NOT NULL,
    cancellation_deadline    TIMESTAMP WITH TIME ZONE NOT NULL,
    net_rate                 NUMERIC(19,4) NOT NULL,
    net_rate_currency        VARCHAR(10) NOT NULL,
    markup_applied           NUMERIC(19,4) NOT NULL,
    currency_buffer_applied  NUMERIC(19,4) NOT NULL,
    sell_rate                NUMERIC(19,4) NOT NULL,
    sell_currency            VARCHAR(10) NOT NULL,
    fx_rate_snapshot         NUMERIC(19,6) NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL
);
