-- Flyway Migration V3: User Fleet Data Isolation
-- Associates vehicles with the user_email of their owner/creator.
-- Existing seed vehicles are owned by the official testcase account ('test@starkgroup.test').

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS user_email VARCHAR(160) NOT NULL DEFAULT 'test@starkgroup.test';
CREATE INDEX IF NOT EXISTS idx_vehicles_user_email ON vehicles (user_email);

-- Ensure vehicle number uniqueness is scoped per user
ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS uk_vehicles_vehicle_number;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_vehicles_user_vehicle_number'
    ) THEN
        ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_user_vehicle_number UNIQUE (user_email, vehicle_number);
    END IF;
END $$;
