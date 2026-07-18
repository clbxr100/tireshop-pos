-- Script to fix the sales table schema
-- Add void-related columns if they don't exist

-- For H2 Database
ALTER TABLE sales ADD COLUMN IF NOT EXISTS isVoided BOOLEAN DEFAULT FALSE;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS voidReason VARCHAR(255);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS voidTimestamp TIMESTAMP;

-- Update existing rows to have isVoided = false
UPDATE sales SET isVoided = FALSE WHERE isVoided IS NULL; 