-- Add task-level review depth configuration.

SET @add_tasks_review_level_count = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE tasks ADD COLUMN review_level_count INT DEFAULT 1 AFTER ai_review_config_id',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tasks'
      AND column_name = 'review_level_count'
);
PREPARE add_tasks_review_level_count_stmt FROM @add_tasks_review_level_count;
EXECUTE add_tasks_review_level_count_stmt;
DEALLOCATE PREPARE add_tasks_review_level_count_stmt;
