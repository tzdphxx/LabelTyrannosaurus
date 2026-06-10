ALTER TABLE tasks
    ADD COLUMN assigned_labeler_id BIGINT NULL
        COMMENT '指派策略下的被指派人id',
    ADD INDEX idx_tasks_assigned_labeler (assigned_labeler_id),
    ADD CONSTRAINT fk_tasks_assigned_labeler
        FOREIGN KEY (assigned_labeler_id) REFERENCES users (id);
