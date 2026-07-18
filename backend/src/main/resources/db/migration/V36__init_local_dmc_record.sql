-- Supplier module, DMC-01/DMC-02/DMC-04/DMC-05/DMC-11, PRD S10.3/S20.14: a
-- Local DMC (a manually onboarded destination-management-company partner,
-- no live API) starts Pending and only becomes Active once a reviewer
-- records at least one verification step. references_info avoids the
-- "references" reserved-ish word some SQL dialects treat specially.
--
-- All columns land in this one migration up front (matching V32's own
-- precedent of creating AI-13's retry columns before AI-13's retry LOGIC
-- existed) since ddl-auto: validate requires the schema and the JPA
-- entity to agree at every commit -- the quality-signal/flag/staleness
-- columns (DMC-04/05/11) are unused until their own story's behavior
-- lands, not unused because they were added early by mistake.

CREATE TABLE local_dmc_record (
    local_dmc_id             UUID PRIMARY KEY,
    consultant_id              UUID NOT NULL,
    business_name                 VARCHAR(255) NOT NULL,
    product_categories               VARCHAR(500) NOT NULL,
    sample_rates_summary                TEXT,
    references_info                        TEXT,
    status                                    VARCHAR(20) NOT NULL,
    verification_notes                          TEXT,
    total_bookings_count                          INTEGER NOT NULL DEFAULT 0,
    cancelled_bookings_count                        INTEGER NOT NULL DEFAULT 0,
    cancellation_rate                                 NUMERIC(6,4) NOT NULL DEFAULT 0,
    complaint_count                                     INTEGER NOT NULL DEFAULT 0,
    flagged                                               BOOLEAN NOT NULL DEFAULT FALSE,
    inventory_stale                                         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                                                TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_local_dmc_record_consultant_id ON local_dmc_record (consultant_id);
