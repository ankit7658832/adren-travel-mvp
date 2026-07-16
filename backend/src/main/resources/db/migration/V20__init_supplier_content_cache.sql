-- BOK-27: cached static supplier content (property/ship/activity name,
-- rating, description as applicable — PRD Section 10.5). Keyed by
-- (supplier_id, supplier_content_id) so the same physical content item is
-- refreshed in place, not duplicated, across sync runs.

CREATE TABLE supplier_content_cache (
    id                  UUID PRIMARY KEY,
    supplier_id         VARCHAR(30) NOT NULL,
    supplier_content_id VARCHAR(200) NOT NULL,
    name                VARCHAR(500),
    rating              DOUBLE PRECISION,
    last_synced_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_supplier_content_cache_key UNIQUE (supplier_id, supplier_content_id)
);
