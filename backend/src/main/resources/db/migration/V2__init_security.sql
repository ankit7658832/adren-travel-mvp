-- Initial schema for the Security module (RULES.md Section 5;
-- PRD Section 6 Roles & Permissions Matrix).
-- Backs CapabilityGrantService's data-driven "No (unless granted)" checks
-- (e.g. a USER creating a Package) — one row per (user_id, capability),
-- presence of the row with granted = true is the grant.

CREATE TABLE capability_grant (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    capability  VARCHAR(50) NOT NULL,
    granted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX idx_capability_grant_user_capability
    ON capability_grant (user_id, capability);
