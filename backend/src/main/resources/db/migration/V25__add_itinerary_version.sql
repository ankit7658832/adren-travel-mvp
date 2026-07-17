-- BOK-16: optimistic-locking column for Itinerary, closing PRD Section
-- 23.1 Edge Case #1 (two concurrent confirmations of the same
-- itinerary must not both succeed).
ALTER TABLE itinerary ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
