-- BOK-18: traveler_count on Quotation (PRD §23.1 Edge Case #3). Defaults
-- to 1 for existing rows and every new Quotation created by saveAsQuotation
-- today — BOK-09 never captured a traveler count at quotation time
-- (documented assumption, same shape as BOK-09's own valid_until default).
ALTER TABLE quotation ADD COLUMN traveler_count INTEGER NOT NULL DEFAULT 1;
