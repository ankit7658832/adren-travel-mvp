-- AI-06, PRD S11.2 principle 3: an AI-generated itinerary cannot become a
-- Quotation until this is explicitly set true (Itinerary.markAsQuotation's
-- new gate) — separate from ai_generated/ai_audit_log_id (V1), which only
-- record that AI touched this itinerary at all, not that a human approved it.
ALTER TABLE itinerary ADD COLUMN ai_approved BOOLEAN NOT NULL DEFAULT FALSE;
