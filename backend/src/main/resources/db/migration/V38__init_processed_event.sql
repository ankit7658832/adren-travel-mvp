-- HRD-03 / RULES.md §2.2 — dedup guard for @ApplicationModuleListener
-- redelivery: a listener claims (event_id, listener_name) via this table's
-- unique constraint before dispatching, so an at-least-once redelivery of
-- the same event to the same listener is a no-op, not a duplicate side effect.
CREATE TABLE processed_event (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    listener_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_processed_event_event_id_listener UNIQUE (event_id, listener_name)
);
