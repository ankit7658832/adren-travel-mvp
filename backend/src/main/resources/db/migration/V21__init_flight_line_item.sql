-- Booking module: Flight line item (PRD Section 20.3, Section 10.2.4 — BOK-04).
-- Same shape as hotel_line_item (V14) minus cancellation_deadline — flight
-- fares expire on their own faster clock, handled at booking time (AI-09),
-- not stored as a per-line-item deadline.

CREATE TABLE flight_line_item (
    line_item_id             UUID PRIMARY KEY,
    itinerary_id             UUID NOT NULL REFERENCES itinerary(itinerary_id),
    supplier_id              VARCHAR(20) NOT NULL,
    supplier_rate_id         VARCHAR(255) NOT NULL,
    airline_code             VARCHAR(10) NOT NULL,
    flight_number            VARCHAR(20) NOT NULL,
    cabin_class               VARCHAR(20) NOT NULL,
    baggage_allowance        VARCHAR(100),
    net_rate                 NUMERIC(19,4) NOT NULL,
    net_rate_currency        VARCHAR(10) NOT NULL,
    markup_applied           NUMERIC(19,4) NOT NULL,
    currency_buffer_applied  NUMERIC(19,4) NOT NULL,
    sell_rate                NUMERIC(19,4) NOT NULL,
    sell_currency             VARCHAR(10) NOT NULL,
    fx_rate_snapshot         NUMERIC(19,6) NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL
);
