-- Review traceability: allow AI_REJECT action in review_records
ALTER TABLE review_records MODIFY COLUMN `action` VARCHAR(30) NOT NULL;
