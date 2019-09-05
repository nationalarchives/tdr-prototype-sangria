/**
  Switch the file table from using string last_modified_dates to timestamp with timezone to store dates correctly.
 */

ALTER TABLE file
  ALTER COLUMN last_modified_date
  TYPE TIMESTAMPTZ USING last_modified_date::TIMESTAMPTZ;