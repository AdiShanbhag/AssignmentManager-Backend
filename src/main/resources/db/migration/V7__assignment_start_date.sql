-- By Claude - Adds optional start date to assignments for semester planning (Story 12.1)
ALTER TABLE assignments ADD COLUMN start_date DATE NULL;