SET @drop_tasks_quota_check = (
    SELECT IF(COUNT(*) = 1,
        'ALTER TABLE tasks DROP CHECK chk_tasks_quota',
        'SELECT 1')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'tasks'
      AND constraint_name = 'chk_tasks_quota'
      AND constraint_type = 'CHECK'
);
PREPARE stmt FROM @drop_tasks_quota_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_tasks_quota_check = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE tasks ADD CONSTRAINT chk_tasks_quota CHECK (quota >= 0)',
        'SELECT 1')
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'tasks'
      AND constraint_name = 'chk_tasks_quota'
      AND constraint_type = 'CHECK'
);
PREPARE stmt FROM @add_tasks_quota_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
