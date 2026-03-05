-- By Claude - Adds notification preference columns to users table for Story 11.1
ALTER TABLE users ADD COLUMN notifications_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN daily_reminder_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN daily_reminder_time VARCHAR(5) NOT NULL DEFAULT '08:00';
ALTER TABLE users ADD COLUMN due_date_warning_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN due_date_warning_days_before INT NOT NULL DEFAULT 2;
ALTER TABLE users ADD COLUMN at_risk_alert_enabled BOOLEAN NOT NULL DEFAULT true;