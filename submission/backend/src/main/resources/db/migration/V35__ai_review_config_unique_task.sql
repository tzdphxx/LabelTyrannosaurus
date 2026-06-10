-- V35: Add UNIQUE constraint on ai_review_configs.task_id
-- Prevents duplicate AI review configs for the same task at DB level.
-- Before adding the constraint, deduplicate by keeping the latest row (max id).

DELETE FROM ai_review_configs
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MAX(id) AS id FROM ai_review_configs GROUP BY task_id
    ) AS kept
);

SET @add_ai_review_configs_task_unique = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE ai_review_configs ADD UNIQUE KEY uk_ai_review_configs_task (task_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_review_configs'
      AND index_name = 'uk_ai_review_configs_task'
);
PREPARE stmt FROM @add_ai_review_configs_task_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
