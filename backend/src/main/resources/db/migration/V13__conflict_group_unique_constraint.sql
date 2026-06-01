SET @add_conflict_groups_task_item_unique = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE conflict_groups ADD CONSTRAINT uk_conflict_groups_task_item UNIQUE (task_id, dataset_item_id)',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'conflict_groups'
      AND index_name = 'uk_conflict_groups_task_item'
);
PREPARE add_conflict_groups_task_item_unique_stmt FROM @add_conflict_groups_task_item_unique;
EXECUTE add_conflict_groups_task_item_unique_stmt;
DEALLOCATE PREPARE add_conflict_groups_task_item_unique_stmt;
