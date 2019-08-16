CREATE TABLE series (
  id SERIAL PRIMARY KEY,
  name VARCHAR (50) NOT NULL,
  description VARCHAR (4000) NOT NULL
);

ALTER TABLE consignments
  ADD COLUMN series_id INT NOT NULL,
  ADD CONSTRAINT consignment_series_fk FOREIGN KEY (series_id)
    REFERENCES series (id) ON DELETE CASCADE;