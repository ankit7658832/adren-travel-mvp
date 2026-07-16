-- Booking module: Cruise line item (PRD Section 20.5, Section 10.2.6 — BOK-06).
-- cruise_line_item_port is the @ElementCollection table for Widgety's
-- multi-port itinerary structure, flattened as metadata on the line item
-- rather than separate line items (Section 10.2.6's explicit flattening
-- rule) — mirrors traveler_profile_document's (V11) own @ElementCollection
-- table shape.

CREATE TABLE cruise_line_item (
    line_item_id                  UUID PRIMARY KEY,
    itinerary_id                  UUID NOT NULL REFERENCES itinerary(itinerary_id),
    supplier_id                   VARCHAR(20) NOT NULL,
    supplier_rate_id              VARCHAR(255) NOT NULL,
    cruise_line                   VARCHAR(255) NOT NULL,
    cabin_category                 VARCHAR(100) NOT NULL,
    passenger_documents_required  BOOLEAN NOT NULL,
    net_rate                      NUMERIC(19,4) NOT NULL,
    net_rate_currency             VARCHAR(10) NOT NULL,
    markup_applied                NUMERIC(19,4) NOT NULL,
    currency_buffer_applied       NUMERIC(19,4) NOT NULL,
    sell_rate                     NUMERIC(19,4) NOT NULL,
    sell_currency                  VARCHAR(10) NOT NULL,
    fx_rate_snapshot              NUMERIC(19,6) NOT NULL,
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cruise_line_item_port (
    line_item_id  UUID NOT NULL REFERENCES cruise_line_item(line_item_id),
    port          VARCHAR(255) NOT NULL
);
