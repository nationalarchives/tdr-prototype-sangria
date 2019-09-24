/**
  Add creator - use Cognito user name for Alpha.
 */

ALTER TABLE consignments ADD COLUMN creator VARCHAR (50) DEFAULT 'creator';
ALTER TABLE consignments ALTER COLUMN creator SET NOT NULL;
ALTER TABLE consignments  ADD COLUMN transferring_body VARCHAR (50) DEFAULT 'transferring body';
ALTER TABLE consignments ALTER COLUMN transferring_body SET NOT NULL;
