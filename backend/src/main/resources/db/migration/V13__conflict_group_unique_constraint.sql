ALTER TABLE conflict_groups
    ADD CONSTRAINT uk_conflict_groups_task_item
        UNIQUE (task_id, dataset_item_id);
