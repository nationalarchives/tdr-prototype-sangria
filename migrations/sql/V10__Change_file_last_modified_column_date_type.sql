/**
  Switch the file table from using string last_modified_dates to timestamp with timezone to store dates correctly.
 */

ALTER TABLE file ADD COLUMN placeholder TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE file ALTER COLUMN placeholder SET NOT NULL;
ALTER TABLE file DROP COLUMN last_modified_date;
ALTER TABLE file RENAME COLUMN placeholder TO last_modified_date;