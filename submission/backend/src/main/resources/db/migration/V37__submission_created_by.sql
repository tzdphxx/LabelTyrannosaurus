ALTER TABLE submissions
    ADD COLUMN created_by BIGINT NULL
    AFTER labeler_id;
