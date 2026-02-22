-- Add user_id columns (nullable first so we can backfill safely)
ALTER TABLE assignments ADD COLUMN user_id UUID;
ALTER TABLE availability ADD COLUMN user_id UUID;

-- Backfill existing rows to the oldest user (dev safety)
UPDATE assignments
SET user_id = (SELECT id FROM users ORDER BY created_at LIMIT 1)
WHERE user_id IS NULL;

UPDATE availability
SET user_id = (SELECT id FROM users ORDER BY created_at LIMIT 1)
WHERE user_id IS NULL;

-- Enforce NOT NULL now that data is backfilled
ALTER TABLE assignments ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE availability ALTER COLUMN user_id SET NOT NULL;

-- Foreign keys
ALTER TABLE assignments
    ADD CONSTRAINT fk_assignments_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;

ALTER TABLE availability
    ADD CONSTRAINT fk_availability_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;

-- Indexes for scoped queries
CREATE INDEX idx_assignments_user_id ON assignments(user_id);
CREATE INDEX idx_availability_user_id ON availability(user_id);

-- Optional but good: one availability row per user
CREATE UNIQUE INDEX uq_availability_user_id ON availability(user_id);