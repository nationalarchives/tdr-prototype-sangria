CREATE TABLE file_format (
  id SERIAL PRIMARY KEY,
  pronom_id VARCHAR (255) NOT NULL,
  file_id INT NOT NULL REFERENCES file (id)
);