-- Booking module: Package, converted from a Quotation (PRD Section 20.7,
-- Section 9.1 Flow B — BOK-10). Table named travel_package (not "package")
-- for symmetry with the Java entity's TravelPackage name. source_itinerary_id
-- is a real FK since Itinerary is owned by this SAME module (RULES.md
-- Section 4.2's "value not constraint" rule is about cross-module
-- references only).

CREATE TABLE travel_package (
    package_id                 UUID PRIMARY KEY,
    source_itinerary_id        UUID NOT NULL REFERENCES itinerary(itinerary_id),
    consultant_id              UUID NOT NULL,
    name                       VARCHAR(255) NOT NULL,
    description                TEXT,
    validity_start             DATE NOT NULL,
    validity_end               DATE NOT NULL,
    base_price                 NUMERIC(19,4) NOT NULL,
    markup_price               NUMERIC(19,4) NOT NULL,
    currency                   VARCHAR(10) NOT NULL,
    max_pax                    INTEGER NOT NULL,
    promoted_via_ads           BOOLEAN NOT NULL DEFAULT FALSE,
    ad_campaign_id             UUID,
    dynamic_flight_hotel_combo BOOLEAN NOT NULL DEFAULT FALSE,
    status                     VARCHAR(20) NOT NULL,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL
);
