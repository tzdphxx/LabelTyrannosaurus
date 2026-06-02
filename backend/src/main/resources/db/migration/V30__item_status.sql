-- V30: add status column on dataset_items
ALTER TABLE dataset_items
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' AFTER assigned_count;

UPDATE dataset_items
SET status = 'FULL'
WHERE deleted = 0
  AND assigned_count >= 1;
