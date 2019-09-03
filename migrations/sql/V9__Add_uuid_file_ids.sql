/**
  Switch the file table from using numeric IDs to UUIDs, so that files can be given matching UUID names in S3.
 */

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE file
  ADD COLUMN file_uuid uuid NOT NULL DEFAULT uuid_generate_v1();
CREATE UNIQUE INDEX unique_file_id ON file (file_uuid);

ALTER TABLE file_format
  ADD COLUMN file_uuid uuid REFERENCES file (file_uuid);
ALTER TABLE file_metadata
  ADD COLUMN file_uuid uuid REFERENCES file (file_uuid);
ALTER TABLE file_status
  ADD COLUMN file_uuid uuid REFERENCES file (file_uuid);

UPDATE file_format
  SET file_uuid = file.file_uuid
  FROM file
  WHERE file_format.file_id = file.id;
UPDATE file_metadata
  SET file_uuid = file.file_uuid
  FROM file
  WHERE file_metadata.file_id = file.id;
UPDATE file_status
  SET file_uuid = file.file_uuid
  FROM file
  WHERE file_status.file_id = file.id;

ALTER TABLE file_format ALTER COLUMN file_uuid SET NOT NULL;
ALTER TABLE file_metadata ALTER COLUMN file_uuid SET NOT NULL;
ALTER TABLE file_status ALTER COLUMN file_uuid SET NOT NULL;

ALTER TABLE file_format DROP COLUMN file_id;
ALTER TABLE file_metadata DROP COLUMN file_id;
ALTER TABLE file_status DROP COLUMN file_id;

ALTER TABLE file DROP CONSTRAINT file_pkey;
ALTER TABLE file ADD CONSTRAINT file_pkey PRIMARY KEY USING INDEX unique_file_id;
ALTER TABLE file DROP COLUMN id;

ALTER TABLE file_format RENAME COLUMN file_uuid TO file_id;
ALTER TABLE file_metadata RENAME COLUMN file_uuid TO file_id;
ALTER TABLE file_status RENAME COLUMN file_uuid TO file_id;

ALTER TABLE file RENAME COLUMN file_uuid TO id;

COMMIT;