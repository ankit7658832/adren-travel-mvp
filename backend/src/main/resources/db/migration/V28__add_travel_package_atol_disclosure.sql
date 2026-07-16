-- BOK-11: UK ATOL disclosure-completion gate on publish (PRD §17.2, §22.3 T5).
ALTER TABLE travel_package ADD COLUMN atol_disclosure_completed BOOLEAN NOT NULL DEFAULT FALSE;
