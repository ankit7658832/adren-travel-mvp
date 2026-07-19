-- HRD-13: the persisted, queryable staleness signal a Super Admin alert
-- screen surfaces (PRD §10.5) — same shape as V36's local_dmc_record
-- inventory_stale flag, since no Super Admin-addressable notification
-- channel exists anywhere in this codebase.

ALTER TABLE supplier_content_cache ADD COLUMN stale BOOLEAN NOT NULL DEFAULT FALSE;
