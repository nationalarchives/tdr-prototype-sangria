/**
  Switch the file table from using string last_modified_dates to timestamp to store dates correctly.
 */

ALTER TABLE file
ALTER COLUMN last_modified_date
TYPE TIMESTAMP USING last_modified_date::TIMESTAMP;