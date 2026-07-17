-- Booking module: Transfer line item (PRD Section 20.4, Section 10.2.5 — BOK-05).
-- pickup_point/dropoff_point are plain location strings, not FKs into a
-- geocoded-location-entry table — no such entity exists elsewhere in this
-- schema yet (see AddTransferLineItemCommand's Javadoc).

CREATE TABLE transfer_line_item (
    line_item_id             UUID PRIMARY KEY,
    itinerary_id             UUID NOT NULL REFERENCES itinerary(itinerary_id),
    supplier_id              VARCHAR(20) NOT NULL,
    supplier_rate_id         VARCHAR(255) NOT NULL,
    vehicle_type              VARCHAR(50) NOT NULL,
    pickup_point              VARCHAR(255) NOT NULL,
    dropoff_point             VARCHAR(255) NOT NULL,
    net_rate                 NUMERIC(19,4) NOT NULL,
    net_rate_currency        VARCHAR(10) NOT NULL,
    markup_applied           NUMERIC(19,4) NOT NULL,
    currency_buffer_applied  NUMERIC(19,4) NOT NULL,
    sell_rate                NUMERIC(19,4) NOT NULL,
    sell_currency             VARCHAR(10) NOT NULL,
    fx_rate_snapshot         NUMERIC(19,6) NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL
);
