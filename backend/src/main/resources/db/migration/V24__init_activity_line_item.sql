-- Booking module: Activity line item (PRD Section 20.6, Section 10.2.7 — BOK-07).
-- time_slot is a TIME (not a date range) — activity availability is
-- time-slot based per Section 10.2.7.

CREATE TABLE activity_line_item (
    line_item_id             UUID PRIMARY KEY,
    itinerary_id             UUID NOT NULL REFERENCES itinerary(itinerary_id),
    supplier_id              VARCHAR(20) NOT NULL,
    supplier_rate_id         VARCHAR(255) NOT NULL,
    duration_minutes         INTEGER NOT NULL,
    time_slot                 TIME NOT NULL,
    headcount                 INTEGER NOT NULL,
    net_rate                 NUMERIC(19,4) NOT NULL,
    net_rate_currency        VARCHAR(10) NOT NULL,
    markup_applied           NUMERIC(19,4) NOT NULL,
    currency_buffer_applied  NUMERIC(19,4) NOT NULL,
    sell_rate                NUMERIC(19,4) NOT NULL,
    sell_currency             VARCHAR(10) NOT NULL,
    fx_rate_snapshot         NUMERIC(19,6) NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL
);
