/**
  Add creator - use Cognito user name for Alpha.
 */

ALTER TABLE consignments
  ADD COLUMN creator VARCHAR (50) NOT NULL,
  ADD COLUMN transferring_body VARCHAR (50) NOT NULL;
