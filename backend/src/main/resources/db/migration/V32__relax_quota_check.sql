ALTER TABLE tasks
    DROP CHECK chk_tasks_quota;

ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_quota CHECK (quota >= 0);
