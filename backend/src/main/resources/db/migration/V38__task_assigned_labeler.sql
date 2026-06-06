ALTER TABLE tasks
    ADD COLUMN assigned_labeler_id BIGINT NULL
        COMMENT 'Default labeler for ASSIGNED claim strategy auto dispatch',
    ADD INDEX idx_tasks_assigned_labeler (assigned_labeler_id),
    ADD CONSTRAINT fk_tasks_assigned_labeler
        FOREIGN KEY (assigned_labeler_id) REFERENCES users (id);
