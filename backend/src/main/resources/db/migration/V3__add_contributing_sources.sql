-- V3__add_contributing_sources.sql

-- 1. Add the new column (nullable initially so we don't break existing rows)
ALTER TABLE incidents ADD COLUMN contributing_sources VARCHAR(255);

-- 2. Backfill existing data using the comma-wrapping convention
UPDATE incidents SET contributing_sources = ',' || source || ',';

-- 3. Enforce the NOT NULL constraint now that all 215 existing rows are safely populated
ALTER TABLE incidents ALTER COLUMN contributing_sources SET NOT NULL;