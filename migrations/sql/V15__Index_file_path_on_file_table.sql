/**
  Index file path in file table to optimize pagination queries
 */

CREATE INDEX idx_path ON file USING btree(path);