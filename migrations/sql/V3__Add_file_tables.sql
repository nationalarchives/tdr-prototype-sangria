CREATE TABLE file (
  id SERIAL PRIMARY KEY,
  path VARCHAR (5000) NOT NULL,
  consignment_id INT NOT NULL REFERENCES consignments (id)
);

CREATE TABLE file_status (
  id SERIAL PRIMARY KEY,
  checksum_verified BOOLEAN NOT NULL,
  antivirus_passed BOOLEAN NOT NULL,
  file_format_verified BOOLEAN NOT NULL,
  file_id INT NOT NULL REFERENCES file (id)
);

CREATE TABLE file_metadata (
  id SERIAL PRIMARY KEY,
  key VARCHAR(50) NOT NULL,
  value VARCHAR(5000) NOT NULL,
  file_id INT NOT NULL REFERENCES file (id)
);